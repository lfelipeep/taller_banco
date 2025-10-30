import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CuentaBancaria {
    private static final AtomicInteger SEQ = new AtomicInteger(1);

    public enum TipoCuenta { CORRIENTE, AHORROS }

    private final int id;
    private final String cliente;
    private final TipoCuenta tipo;
    private double saldo;
    private final List<Transaccion> historial;

    public CuentaBancaria(String cliente, TipoCuenta tipo, double saldoInicial) {
        this.id = SEQ.getAndIncrement();
        this.cliente = Objects.requireNonNull(cliente, "Cliente no puede ser null");
        this.tipo = Objects.requireNonNull(tipo, "Tipo de cuenta no puede ser null");
        this.saldo = Math.max(0.0, saldoInicial);
        this.historial = new ArrayList<>();
        this.registrarTransaccion("APERTURA", saldoInicial, "Apertura de cuenta");
    }

    public int getId() { return id; }
    public String getCliente() { return cliente; }
    public TipoCuenta getTipo() { return tipo; }

    public synchronized double getSaldo() { return saldo; }

    public synchronized void depositar(double cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad a depositar debe ser mayor que 0");
        saldo += cantidad;
        registrarTransaccion("DEPOSITO", cantidad, "Depósito realizado");
    }

    public synchronized void retirar(double cantidad) throws InsufficientFundsException {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad a retirar debe ser mayor que 0");
        if (cantidad > saldo) throw new InsufficientFundsException("Saldo insuficiente");
        saldo -= cantidad;
        registrarTransaccion("RETIRO", -cantidad, "Retiro realizado");
    }

    // Nueva funcionalidad: Historial de transacciones
    public synchronized List<Transaccion> getHistorial() {
        return Collections.unmodifiableList(new ArrayList<>(historial));
    }

    private synchronized void registrarTransaccion(String tipo, double monto, String descripcion) {
        Transaccion transaccion = new Transaccion(tipo, monto, saldo, descripcion);
        historial.add(transaccion);
    }

    // Nueva funcionalidad: Aplicar intereses para cuentas de ahorros
    public synchronized void aplicarInteres(double tasa) {
        if (tasa <= 0) throw new IllegalArgumentException("La tasa de interés debe ser mayor que 0");
        double interes = saldo * (tasa / 100);
        saldo += interes;
        registrarTransaccion("INTERES", interes, String.format("Interés aplicado %.2f%%", tasa));
    }

    // Nueva funcionalidad: Aplicar cargo para cuentas corrientes
    public synchronized void aplicarCargo(double cargo) {
        if (cargo <= 0) throw new IllegalArgumentException("El cargo debe ser mayor que 0");
        if (cargo > saldo) throw new IllegalArgumentException("Saldo insuficiente para aplicar cargo");
        saldo -= cargo;
        registrarTransaccion("CARGO", -cargo, "Cargo mensual aplicado");
    }

    @Override
    public String toString() {
        return String.format("ID:%d - %s (%s) - Saldo: %.2f", id, cliente, tipo, saldo);
    }

    // Clase para el historial de transacciones
    public static class Transaccion {
        private final String tipo;
        private final double monto;
        private final double saldoResultante;
        private final LocalDateTime fecha;
        private final String descripcion;

        public Transaccion(String tipo, double monto, double saldoResultante, String descripcion) {
            this.tipo = tipo;
            this.monto = monto;
            this.saldoResultante = saldoResultante;
            this.fecha = LocalDateTime.now();
            this.descripcion = descripcion;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return String.format("[%s] %s: %s %.2f | Saldo: %.2f", 
                fecha.format(formatter), tipo, monto >= 0 ? "+" : "", monto, saldoResultante);
        }
    }

    // Excepción específica para manejo claro
    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String msg) { super(msg); }
    }

    // Repositorio simple en memoria (separa persistencia del modelo)
    static class Banco {
        private final Map<Integer, CuentaBancaria> cuentas = new LinkedHashMap<>();

        public CuentaBancaria crearCuenta(String cliente, TipoCuenta tipo, double saldoInicial) {
            CuentaBancaria c = new CuentaBancaria(cliente, tipo, saldoInicial);
            cuentas.put(c.getId(), c);
            return c;
        }

        public Optional<CuentaBancaria> obtenerCuenta(int id) {
            return Optional.ofNullable(cuentas.get(id));
        }

        public Collection<CuentaBancaria> listar() {
            return Collections.unmodifiableCollection(cuentas.values());
        }

        // Nueva funcionalidad: Transferencia entre cuentas
        public synchronized void transferir(int fromId, int toId, double monto) 
                throws IllegalArgumentException, InsufficientFundsException {
            
            if (monto <= 0) {
                throw new IllegalArgumentException("El monto de transferencia debe ser mayor que 0");
            }

            CuentaBancaria cuentaOrigen = cuentas.get(fromId);
            CuentaBancaria cuentaDestino = cuentas.get(toId);

            if (cuentaOrigen == null) {
                throw new IllegalArgumentException("Cuenta origen no encontrada");
            }
            if (cuentaDestino == null) {
                throw new IllegalArgumentException("Cuenta destino no encontrada");
            }
            if (fromId == toId) {
                throw new IllegalArgumentException("No se puede transferir a la misma cuenta");
            }

            // Realizar retiro de cuenta origen
            cuentaOrigen.retirar(monto);
            
            try {
                // Realizar depósito en cuenta destino
                cuentaDestino.depositar(monto);
                
                // Registrar transacciones específicas
                cuentaOrigen.registrarTransaccion("TRANSFERENCIA_ENVIADA", -monto, 
                    String.format("Transferencia a cuenta %d", toId));
                cuentaDestino.registrarTransaccion("TRANSFERENCIA_RECIBIDA", monto, 
                    String.format("Transferencia de cuenta %d", fromId));
                    
            } catch (Exception e) {
                // Revertir en caso de error (rollback)
                cuentaOrigen.depositar(monto);
                throw e;
            }
        }

        // Nueva funcionalidad: Aplicar intereses y cargos a todas las cuentas
        public synchronized void aplicarInteresesYCargos(double tasaAhorros, double cargoCorriente) {
            for (CuentaBancaria cuenta : cuentas.values()) {
                try {
                    if (cuenta.getTipo() == TipoCuenta.AHORROS) {
                        cuenta.aplicarInteres(tasaAhorros);
                    } else {
                        cuenta.aplicarCargo(cargoCorriente);
                    }
                } catch (Exception e) {
                    System.out.printf("Error en cuenta %d: %s%n", cuenta.getId(), e.getMessage());
                }
            }
        }

        // Nueva funcionalidad: Obtener historial de cuenta
        public List<Transaccion> obtenerHistorial(int id) {
            CuentaBancaria cuenta = cuentas.get(id);
            if (cuenta == null) {
                return Collections.emptyList();
            }
            return cuenta.getHistorial();
        }
    }

    // Interacción por consola (I/O separado de lógica)
    public static void main(String[] args) {
        Banco banco = new Banco();
     

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.println();
                System.out.println("********************");
                System.out.println("1 - Crear cuenta");
                System.out.println("2 - Consultar saldo");
                System.out.println("3 - Retirar");
                System.out.println("4 - Depositar");
                System.out.println("5 - Listar cuentas");
                System.out.println("6 - Transferir entre cuentas");
                System.out.println("7 - Ver historial de transacciones");
                System.out.println("8 - Aplicar intereses y cargos");
                System.out.println("9 - Salir");
                System.out.print("Seleccione opción: ");

                String linea = sc.nextLine().trim();
                if (linea.isEmpty()) continue;

                int opcion;
                try { opcion = Integer.parseInt(linea); }
                catch (NumberFormatException e) { System.out.println("Opción inválida."); continue; }

                switch (opcion) {
                    case 1:
                        crearCuentaFlow(sc, banco);
                        break;
                    case 2:
                        consultarSaldoFlow(sc, banco);
                        break;
                    case 3:
                        retirarFlow(sc, banco);
                        break;
                    case 4:
                        depositarFlow(sc, banco);
                        break;
                    case 5:
                        listarFlow(banco);
                        break;
                    case 6:
                        transferirFlow(sc, banco);
                        break;
                    case 7:
                        historialFlow(sc, banco);
                        break;
                    case 8:
                        aplicarInteresesCargosFlow(sc, banco);
                        break;
                    case 9:
                        System.out.println("Saliendo...");
                        return;
                    default:
                        System.out.println("Opción no válida.");
                }
            }
        }
    }

    private static void crearCuentaFlow(Scanner sc, Banco banco) {
        System.out.print("Nombre del titular: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) { System.out.println("Nombre no puede estar vacío."); return; }

        System.out.print("Tipo (1=Corriente, 2=Ahorros): ");
        String t = sc.nextLine().trim();
        TipoCuenta tipo;
        if ("2".equals(t)) tipo = TipoCuenta.AHORROS;
        else tipo = TipoCuenta.CORRIENTE;

        System.out.print("Saldo inicial: ");
        double saldoInicial;
        try { saldoInicial = Double.parseDouble(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Saldo inválido."); return; }

        CuentaBancaria c = banco.crearCuenta(nombre, tipo, saldoInicial);
        System.out.println("Cuenta creada: " + c);
    }

    private static void consultarSaldoFlow(Scanner sc, Banco banco) {
        Optional<CuentaBancaria> o = obtenerCuentaPorId(sc, banco);
        if (o.isPresent()) {
            System.out.println("Saldo: " + o.get().getSaldo());
        } else {
            System.out.println("Cuenta no encontrada.");
        }
    }

    private static void retirarFlow(Scanner sc, Banco banco) {
        Optional<CuentaBancaria> o = obtenerCuentaPorId(sc, banco);
        if (o.isEmpty()) { System.out.println("Cuenta no encontrada."); return; }
        CuentaBancaria c = o.get();

        System.out.print("Cantidad a retirar: ");
        double monto;
        try { monto = Double.parseDouble(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Monto inválido."); return; }

        try {
            c.retirar(monto);
            System.out.println("Retiro exitoso. Nuevo saldo: " + c.getSaldo());
        } catch (InsufficientFundsException e) {
            System.out.println("Operación fallida: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Entrada inválida: " + e.getMessage());
        }
    }

    private static void depositarFlow(Scanner sc, Banco banco) {
        Optional<CuentaBancaria> o = obtenerCuentaPorId(sc, banco);
        if (o.isEmpty()) { System.out.println("Cuenta no encontrada."); return; }
        CuentaBancaria c = o.get();

        System.out.print("Cantidad a depositar: ");
        double monto;
        try { monto = Double.parseDouble(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Monto inválido."); return; }

        try {
            c.depositar(monto);
            System.out.println("Depósito exitoso. Nuevo saldo: " + c.getSaldo());
        } catch (IllegalArgumentException e) {
            System.out.println("Entrada inválida: " + e.getMessage());
        }
    }

    private static void listarFlow(Banco banco) {
        Collection<CuentaBancaria> cuentas = banco.listar();
        if (cuentas.isEmpty()) { System.out.println("No hay cuentas."); return; }
        cuentas.forEach(System.out::println);
    }

    // Nuevos flujos para las funcionalidades agregadas
    private static void transferirFlow(Scanner sc, Banco banco) {
        try {
            System.out.print("ID cuenta origen: ");
            int fromId = Integer.parseInt(sc.nextLine().trim());
            
            System.out.print("ID cuenta destino: ");
            int toId = Integer.parseInt(sc.nextLine().trim());
            
            System.out.print("Monto a transferir: ");
            double monto = Double.parseDouble(sc.nextLine().trim());
            
            banco.transferir(fromId, toId, monto);
            System.out.println("Transferencia exitosa.");
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Entrada inválida - debe ser un número");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void historialFlow(Scanner sc, Banco banco) {
        System.out.print("ID de cuenta para ver historial: ");
        try {
            int id = Integer.parseInt(sc.nextLine().trim());
            List<Transaccion> historial = banco.obtenerHistorial(id);
            
            if (historial.isEmpty()) {
                System.out.println("Cuenta no encontrada o sin transacciones.");
                return;
            }
            
            System.out.println("Historial de transacciones:");
            historial.forEach(System.out::println);
            
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
        }
    }

    private static void aplicarInteresesCargosFlow(Scanner sc, Banco banco) {
        try {
            System.out.print("Tasa de interés para ahorros (%): ");
            double tasaAhorros = Double.parseDouble(sc.nextLine().trim());
            
            System.out.print("Cargo mensual para corrientes: ");
            double cargoCorriente = Double.parseDouble(sc.nextLine().trim());
            
            banco.aplicarInteresesYCargos(tasaAhorros, cargoCorriente);
            System.out.println("Intereses y cargos aplicados exitosamente.");
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Entrada inválida - debe ser un número");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static Optional<CuentaBancaria> obtenerCuentaPorId(Scanner sc, Banco banco) {
        System.out.print("Ingrese ID de cuenta: ");
        String line = sc.nextLine().trim();
        try {
            int id = Integer.parseInt(line);
            return banco.obtenerCuenta(id);
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return Optional.empty();
        }
    }
}