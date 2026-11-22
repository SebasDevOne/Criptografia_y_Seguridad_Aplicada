#!/usr/bin/env python3
"""
Taller: Criptografía y Seguridad Aplicada  -  Parte II, ejercicio 3
Demostración didáctica del cifrado de flujo RC4

AVISO: RC4 está OBSOLETO (sesgos en su keystream; el RFC 7465 prohíbe su uso
en TLS). Este script se usa solo con fines didácticos. El mensaje es ficticio:
no es una credencial ni un token real.

Funcionamiento de RC4:
  1) KSA (Key Scheduling Algorithm): mezcla un arreglo S de 256 bytes usando la clave.
  2) PRGA (Pseudo-Random Generation Algorithm): produce un flujo de bytes (keystream).
  3) Cifrado: cada byte del mensaje se combina con un byte del keystream mediante XOR.
     Como XOR es reversible, descifrar es repetir exactamente la misma operación.

Uso:
  python rc4_demo.py
"""


def ksa(clave: bytes):
    """Key Scheduling Algorithm: inicializa y permuta el arreglo S."""
    S = list(range(256))
    j = 0
    for i in range(256):
        j = (j + S[i] + clave[i % len(clave)]) % 256
        S[i], S[j] = S[j], S[i]
    return S


def prga(S):
    """Generador de keystream: devuelve un byte pseudoaleatorio por iteración."""
    i = j = 0
    while True:
        i = (i + 1) % 256
        j = (j + S[i]) % 256
        S[i], S[j] = S[j], S[i]
        yield S[(S[i] + S[j]) % 256]


def rc4(clave: bytes, datos: bytes) -> bytes:
    """Cifra o descifra (la operación es simétrica) aplicando XOR con el keystream."""
    if not clave:
        raise ValueError("La clave no puede estar vacía.")
    flujo = prga(ksa(clave))
    return bytes(b ^ next(flujo) for b in datos)


def main():
    # Mensaje ficticio de prueba (no es una credencial ni un token real)
    mensaje = "Mensaje ficticio de prueba: reunion de SECURELINK a las 10:00."
    clave = b"ClaveDidactica2026"          # clave definida para la demostración

    print("=== Demostración didáctica de RC4 (cifrado de flujo, OBSOLETO) ===")
    print(f"Mensaje original : {mensaje}")
    print(f"Clave definida   : {clave.decode()}")

    cifrado = rc4(clave, mensaje.encode("utf-8"))
    print(f"Mensaje cifrado  : {cifrado.hex(' ')}")

    descifrado = rc4(clave, cifrado).decode("utf-8")
    print(f"Mensaje descifrado: {descifrado}")
    print("Descifrado correcto:", "SÍ" if descifrado == mensaje else "NO")

    # Con una clave incorrecta no se recupera el mensaje
    incorrecto = rc4(b"ClaveIncorrecta", cifrado)
    print(f"Con clave incorrecta (bytes ilegibles): {incorrecto[:16].hex(' ')} ...")

    print("\nNota: RC4 está obsoleto y se usa aquí solo con fines didácticos.")


if __name__ == "__main__":
    main()