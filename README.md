# Proyecto: Cuenta Bancaria

# Descripción

Este proyecto simula un sistema de cuentas bancarias en consola, desarrollado en Java.
Permite crear cuentas, hacer depósitos, retiros, transferencias, aplicar intereses o cargos mensuales y consultar el historial de transacciones.

Su objetivo es aplicar los principios de la Programación Orientada a Objetos (OOP) como:

Encapsulación: los datos de cada cuenta están protegidos.

Responsabilidad única (SRP): cada clase y método tiene una sola función clara.

Abierto/Cerrado (OCP): se pueden agregar nuevas funciones sin cambiar el código base.

Inversión de dependencias (DIP): la clase Banco maneja las cuentas sin depender de una implementación fija.

# Funcionalidades principales

Crear cuentas bancarias

Corriente o de Ahorros

Se asigna un ID único automáticamente.

Depósitos y retiros

Valida montos positivos y fondos suficientes.

Transferencias entre cuentas

Verifica que ambas cuentas existan, que el monto sea válido y que haya saldo suficiente.

Historial de transacciones

Registra depósitos, retiros, transferencias, intereses y cargos.

Muestra fecha, tipo, monto y saldo final.

Aplicar intereses y cargos

Cuentas de ahorro ganan un interés porcentual.

Cuentas corrientes pagan un cargo mensual.

# Cómo funciona

El programa usa las clases principales dentro del mismo archivo:

CuentaBancaria: maneja el saldo, depósitos, retiros e historial.

Banco: administra todas las cuentas y permite transferencias.

Transaccion: guarda los movimientos de cada cuenta.

InsufficientFundsException: controla errores de saldo insuficiente.

El usuario puede interactuar desde la consola mediante un menú de opciones.
