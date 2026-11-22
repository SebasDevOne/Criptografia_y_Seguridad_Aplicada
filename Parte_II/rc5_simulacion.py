#!/usr/bin/env python3
"""
Taller: Criptografía y Seguridad Aplicada  -  Parte II, ejercicio 1
Simulación didáctica de un cifrado de bloque estilo RC5 sobre un archivo .txt

Operaciones básicas usadas (las mismas vistas en clase):
  - Suma modular      (mod 2^32)
  - XOR
  - Rotaciones a la izquierda / derecha (algunas dependientes de los datos)

Parámetros: RC5-32/12/b  ->  palabras de 32 bits, 12 rondas, clave de b bytes.
Bloque de 64 bits (dos palabras A y B). Modo CBC con relleno PKCS#7.

AVISO: es una simulación didáctica. NO es una implementación segura y NO debe
usarse para proteger archivos ni información real.

Uso:
  python rc5_simulacion.py                      (crea un txt de ejemplo y lo cifra)
  python rc5_simulacion.py mensaje.txt --clave "mi clave"
  python rc5_simulacion.py mensaje_cifrado.bin --clave "mi clave" --descifrar
"""

import argparse
import os
import struct
import sys

# ----------------------------------------------------------------------------
# Parámetros de RC5-32/12/b
# ----------------------------------------------------------------------------
W = 32                    # tamaño de palabra en bits
MASK = 0xFFFFFFFF         # 2^32 - 1, para trabajar módulo 2^32
R = 12                    # número de rondas
P32 = 0xB7E15163          # constante mágica Pw (derivada de e)
Q32 = 0x9E3779B9          # constante mágica Qw (derivada del número áureo)
BLOQUE = 8                # 64 bits = 8 bytes
MAGIC = b"RC5S"           # cabecera del archivo cifrado


# ----------------------------------------------------------------------------
# Operaciones básicas
# ----------------------------------------------------------------------------
def rotl(x, s):
    """Rotación circular a la izquierda de una palabra de 32 bits."""
    s %= W
    return ((x << s) | (x >> (W - s))) & MASK


def rotr(x, s):
    """Rotación circular a la derecha de una palabra de 32 bits."""
    s %= W
    return ((x >> s) | (x << (W - s))) & MASK


# ----------------------------------------------------------------------------
# Expansión de la clave: genera la tabla S de 2*(R+1) subclaves
# ----------------------------------------------------------------------------
def expandir_clave(clave: bytes):
    if not 1 <= len(clave) <= 255:
        raise ValueError("La clave debe tener entre 1 y 255 bytes.")

    # 1) Convertir la clave en palabras de 32 bits (little-endian) -> arreglo L
    c = max(1, (len(clave) + 3) // 4)
    L = [0] * c
    for i in range(len(clave) - 1, -1, -1):
        L[i // 4] = ((L[i // 4] << 8) + clave[i]) & MASK

    # 2) Inicializar S con las constantes mágicas
    t = 2 * (R + 1)
    S = [0] * t
    S[0] = P32
    for i in range(1, t):
        S[i] = (S[i - 1] + Q32) & MASK

    # 3) Mezclar S y L (3 pasadas sobre el arreglo más largo)
    i = j = A = B = 0
    for _ in range(3 * max(t, c)):
        A = S[i] = rotl((S[i] + A + B) & MASK, 3)
        B = L[j] = rotl((L[j] + A + B) & MASK, (A + B) & 31)
        i = (i + 1) % t
        j = (j + 1) % c
    return S


# ----------------------------------------------------------------------------
# Cifrado / descifrado de UN bloque de 8 bytes
# ----------------------------------------------------------------------------
def cifrar_bloque(bloque: bytes, S):
    A, B = struct.unpack("<II", bloque)
    A = (A + S[0]) & MASK                     # suma modular con subclave
    B = (B + S[1]) & MASK
    for i in range(1, R + 1):
        A = (rotl(A ^ B, B) + S[2 * i]) & MASK        # XOR + rotación + suma
        B = (rotl(B ^ A, A) + S[2 * i + 1]) & MASK
    return struct.pack("<II", A, B)


def descifrar_bloque(bloque: bytes, S):
    A, B = struct.unpack("<II", bloque)
    for i in range(R, 0, -1):                 # operaciones inversas, en orden inverso
        B = rotr((B - S[2 * i + 1]) & MASK, A) ^ A
        A = rotr((A - S[2 * i]) & MASK, B) ^ B
    B = (B - S[1]) & MASK
    A = (A - S[0]) & MASK
    return struct.pack("<II", A, B)


# ----------------------------------------------------------------------------
# Relleno y modo CBC
# ----------------------------------------------------------------------------
def agregar_relleno(datos: bytes) -> bytes:
    n = BLOQUE - (len(datos) % BLOQUE)
    return datos + bytes([n]) * n


def quitar_relleno(datos: bytes) -> bytes:
    n = datos[-1]
    if not 1 <= n <= BLOQUE or datos[-n:] != bytes([n]) * n:
        raise ValueError("Relleno inválido: clave incorrecta o archivo dañado.")
    return datos[:-n]


def xor_bytes(a: bytes, b: bytes) -> bytes:
    return bytes(x ^ y for x, y in zip(a, b))


def cifrar_datos(datos: bytes, clave: bytes) -> bytes:
    S = expandir_clave(clave)
    iv = os.urandom(BLOQUE)
    previo = iv
    salida = bytearray()
    datos = agregar_relleno(datos)
    for k in range(0, len(datos), BLOQUE):
        bloque = xor_bytes(datos[k:k + BLOQUE], previo)   # CBC: XOR con bloque previo
        previo = cifrar_bloque(bloque, S)
        salida += previo
    return MAGIC + iv + bytes(salida)


def descifrar_datos(contenido: bytes, clave: bytes) -> bytes:
    if contenido[:4] != MAGIC:
        raise ValueError("El archivo no tiene el formato esperado (cabecera RC5S).")
    S = expandir_clave(clave)
    iv, cuerpo = contenido[4:4 + BLOQUE], contenido[4 + BLOQUE:]
    if len(cuerpo) == 0 or len(cuerpo) % BLOQUE:
        raise ValueError("Longitud de datos cifrados inválida.")
    previo = iv
    salida = bytearray()
    for k in range(0, len(cuerpo), BLOQUE):
        bloque = cuerpo[k:k + BLOQUE]
        salida += xor_bytes(descifrar_bloque(bloque, S), previo)
        previo = bloque
    return quitar_relleno(bytes(salida))


# ----------------------------------------------------------------------------
# Programa principal
# ----------------------------------------------------------------------------
TEXTO_EJEMPLO = (
    "Mensaje ficticio de prueba para el taller de SECURELINK.\n"
    "Este texto no contiene informacion real ni credenciales.\n"
)


def main():
    ap = argparse.ArgumentParser(description="Simulación didáctica de cifrado de bloque estilo RC5.")
    ap.add_argument("archivo", nargs="?", default="mensaje.txt", help="archivo de entrada")
    ap.add_argument("--clave", default="clave-didactica-2026", help="clave (texto)")
    ap.add_argument("--descifrar", action="store_true", help="descifrar en vez de cifrar")
    args = ap.parse_args()
    clave = args.clave.encode("utf-8")

    if args.descifrar:
        with open(args.archivo, "rb") as f:
            contenido = f.read()
        try:
            texto = descifrar_datos(contenido, clave)
        except ValueError as e:
            sys.exit(f"Error: {e}")
        base = args.archivo.rsplit(".", 1)[0]
        salida = f"{base}_descifrado.txt"
        with open(salida, "wb") as f:
            f.write(texto)
        print(f"Archivo descifrado guardado en: {salida}")
        print("Contenido:\n" + texto.decode("utf-8", errors="replace"))
        return

    if not os.path.exists(args.archivo):
        with open(args.archivo, "w", encoding="utf-8") as f:
            f.write(TEXTO_EJEMPLO)
        print(f"(No existía {args.archivo}; se creó un archivo de ejemplo.)")

    with open(args.archivo, "rb") as f:
        original = f.read()

    cifrado = cifrar_datos(original, clave)
    base = args.archivo.rsplit(".", 1)[0]
    salida = f"{base}_cifrado.bin"
    with open(salida, "wb") as f:
        f.write(cifrado)

    print("=== Simulación RC5-32/12 (didáctica, no segura) ===")
    print(f"Archivo original : {args.archivo} ({len(original)} bytes)")
    print(f"Archivo cifrado  : {salida} ({len(cifrado)} bytes)")
    print(f"Primeros bytes (hex): {cifrado[:32].hex(' ')}")

    # Verificación automática: descifrar y comparar con el original
    recuperado = descifrar_datos(cifrado, clave)
    print("Verificación de descifrado:", "CORRECTA" if recuperado == original else "FALLÓ")


if __name__ == "__main__":
    main()