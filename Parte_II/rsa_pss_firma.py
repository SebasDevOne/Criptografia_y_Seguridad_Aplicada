#!/usr/bin/env python3
"""
Taller: Criptografía y Seguridad Aplicada  -  Parte II, ejercicio 2
Firma digital y verificación con RSA-PSS y SHA-256

Qué hace:
  1) Genera un par de claves RSA de 2048 bits.
  2) Firma un mensaje (o un archivo) con la clave PRIVADA usando RSA-PSS con SHA-256.
  3) Verifica la firma con la clave PÚBLICA: debe ser válida.
  4) Modifica un solo carácter del mensaje y vuelve a verificar: debe fallar.
  5) Explica qué demuestra el resultado.

Requiere la librería `cryptography`:
  pip install cryptography

Uso:
  python rsa_pss_firma.py                    (firma un mensaje ficticio de prueba)
  python rsa_pss_firma.py --archivo mensaje.txt
"""

import argparse
import sys

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding, rsa

MENSAJE_PRUEBA = "Mensaje ficticio de prueba: SECURELINK autoriza el reporte del viernes."


def esquema_pss():
    """RSA-PSS: relleno probabilístico con MGF1 y SHA-256; la sal tiene el largo del hash."""
    return padding.PSS(mgf=padding.MGF1(hashes.SHA256()), salt_length=hashes.SHA256().digest_size)


def firmar(clave_privada, datos: bytes) -> bytes:
    """Firma con la clave privada: calcula SHA-256 del mensaje y lo firma con RSA-PSS."""
    return clave_privada.sign(datos, esquema_pss(), hashes.SHA256())


def verificar(clave_publica, firma: bytes, datos: bytes) -> bool:
    """Verifica con la clave pública. Devuelve True si la firma corresponde a esos datos."""
    try:
        clave_publica.verify(firma, datos, esquema_pss(), hashes.SHA256())
        return True
    except InvalidSignature:
        return False


def alterar_un_caracter(datos: bytes) -> bytes:
    """Cambia un solo byte (el primero) del mensaje."""
    primero = datos[0] ^ 0x01          # invierte un bit: p. ej. 'M' se vuelve 'L'
    return bytes([primero]) + datos[1:]


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    ap = argparse.ArgumentParser(description="Firma digital con RSA-PSS y SHA-256.")
    ap.add_argument("--archivo", help="archivo a firmar (si no se indica, se usa un mensaje de prueba)")
    args = ap.parse_args()

    if args.archivo:
        with open(args.archivo, "rb") as f:
            datos = f.read()
        etiqueta = f"archivo {args.archivo} ({len(datos)} bytes)"
    else:
        datos = MENSAJE_PRUEBA.encode("utf-8")
        etiqueta = f"mensaje: {MENSAJE_PRUEBA}"
    if not datos:
        sys.exit("Error: no hay datos para firmar.")

    print("=== Firma digital RSA-PSS con SHA-256 ===")
    clave_privada = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    clave_publica = clave_privada.public_key()
    print("Par de claves RSA de 2048 bits generado (la clave privada solo vive en memoria).")
    print(f"Se firma el {etiqueta}")

    firma = firmar(clave_privada, datos)
    print(f"\nFirma ({len(firma)} bytes), primeros 32 en hex:\n  {firma[:32].hex(' ')} ...")

    print("\n1) Verificación con el mensaje original")
    ok = verificar(clave_publica, firma, datos)
    print("   Resultado:", "FIRMA VÁLIDA" if ok else "FIRMA INVÁLIDA")

    alterado = alterar_un_caracter(datos)
    print("\n2) Verificación después de modificar UN carácter del mensaje")
    if not args.archivo:
        print(f"   Original : {datos.decode('utf-8')}")
        print(f"   Alterado : {alterado.decode('utf-8', errors='replace')}")
    ok2 = verificar(clave_publica, firma, alterado)
    print("   Resultado:", "FIRMA VÁLIDA" if ok2 else "FIRMA INVÁLIDA")

    print("\n¿Qué demuestra el resultado?")
    print("  - Con el mensaje intacto la firma es válida: el mensaje no cambió (INTEGRIDAD) y solo")
    print("    pudo firmarlo quien tiene la clave privada (AUTENTICIDAD).")
    print("  - Al cambiar un solo carácter, el SHA-256 del mensaje es otro y la verificación falla.")
    print("  - La firma NO cifra ni oculta el mensaje: este sigue siendo legible por cualquiera.")
    print("    Para confidencialidad habría que cifrarlo aparte (p. ej. con AES).")


if __name__ == "__main__":
    main()
