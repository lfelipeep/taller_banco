#  Proyecto: Sistema de Cuentas Bancarias

##  Descripción
Sistema de gestión bancaria desarrollado en Java que simula operaciones básicas de cuentas corrientes y de ahorros. El proyecto implementa principios de Programación Orientada a Objetos (OOP) con un enfoque en código mantenible y escalable.

##  Objetivos del Proyecto
Aplicar principios SOLID y OOP:
- **Encapsulación**: Datos protegidos con acceso controlado
- **Responsabilidad Única (SRP)**: Cada clase con una función específica
- **Abierto/Cerrado (OCP)**: Extensible sin modificar código existente
- **Inversión de Dependencias (DIP)**: Banco maneja cuentas de forma abstracta

##  Funcionalidades Principales

###  Gestión de Cuentas
- Crear cuentas (Corriente o Ahorros)
- ID único automático
- Consulta de saldo

###  Operaciones Bancarias
- **Depósitos**: Validación de montos positivos
- **Retiros**: Control de fondos suficientes
- **Transferencias**: Entre cuentas con validación completa

###  Historial de Transacciones
- Registro automático de todas las operaciones
- Fecha, tipo, monto y saldo resultante
- Consulta de historial por cuenta

### Intereses y Cargos
- **Cuentas de Ahorro**: Aplicación de intereses porcentuales
- **Cuentas Corrientes**: Cargo mensual fijo

##  Arquitectura del Proyecto

### Clases Principales
```java
CuentaBancaria          // Gestión de saldo y operaciones individuales
├── Banco              // Administración central de cuentas
├── Transaccion        // Registro de movimientos
└── InsufficientFundsException  // Manejo de errores
