import java.util.*;

public class SimuladorRedeFilas {

    // --- CLASSES DO DOMÍNIO ---
    static class RandomGenerator {
        private Random random = new Random(12345); // Semente fixa para reprodutibilidade
        private int count = 0;
        private final int limit;

        public RandomGenerator(int limit) {
            this.limit = limit;
        }

        public double nextDouble() {
            if (count >= limit) return -1; // Sinaliza fim da simulação
            count++;
            return random.nextDouble();
        }

        public boolean limitReached() {
            return count >= limit;
        }
    }

    enum EventType { CHEGADA, SAIDA }

    static class Event implements Comparable<Event> {
        EventType type;
        double time;
        int queueId;

        public Event(EventType type, double time, int queueId) {
            this.type = type;
            this.time = time;
            this.queueId = queueId;
        }

        @Override
        public int compareTo(Event o) {
            return Double.compare(this.time, o.time);
        }
    }

    static class Fila {
        int id;
        int servidores;
        int capacidade;
        double minAtendimento, maxAtendimento;

        int estadoAtual = 0;
        int perdas = 0;
        double[] tempoAcumulado;

        public Fila(int id, int servidores, int capacidade, double minAtendimento, double maxAtendimento) {
            this.id = id;
            this.servidores = servidores;
            this.capacidade = capacidade;
            this.minAtendimento = minAtendimento;
            this.maxAtendimento = maxAtendimento;
            // O array de tempo acumulado precisa ir de 0 até a capacidade
            this.tempoAcumulado = new double[(capacidade == Integer.MAX_VALUE ? 1000 : capacidade) + 1];
        }

        public void atualizarTempo(double deltaTempo) {
            if (estadoAtual < tempoAcumulado.length) {
                tempoAcumulado[estadoAtual] += deltaTempo;
            }
        }
    }

    // --- VARIÁVEIS GLOBAIS DA SIMULAÇÃO ---
    static PriorityQueue<Event> escalonador;
    static double tempoGlobal = 0.0;
    static Map<Integer, Fila> filas;
    static RandomGenerator rnd;

    // --- LÓGICA PRINCIPAL ---
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" INICIANDO SIMULAÇÃO T1 (100.000 ALEATÓRIOS)");
        System.out.println("==================================================");
        executarCenarioT1();

        System.out.println("\n==================================================");
        System.out.println(" MODO INTERATIVO PARA NOVOS TESTES");
        System.out.println("==================================================");
        iniciarModoUsuario();
    }

    private static void executarCenarioT1() {
        // Inicializa RNG para 100.000 números
        rnd = new RandomGenerator(100000);
        escalonador = new PriorityQueue<>();
        tempoGlobal = 0.0;
        filas = new HashMap<>();

        // Configuração das filas conforme especificação T1
        filas.put(1, new Fila(1, 1, Integer.MAX_VALUE, 1.0, 2.0)); // G/G/1
        filas.put(2, new Fila(2, 2, 5, 4.0, 6.0));                 // G/G/2/5
        filas.put(3, new Fila(3, 2, 10, 5.0, 15.0));               // G/G/2/10

        // Primeiro evento: Chegada na Fila 1 no tempo 2.0
        escalonador.add(new Event(EventType.CHEGADA, 2.0, 1));

        // Loop Principal
        while (!escalonador.isEmpty() && !rnd.limitReached()) {
            Event eventoAtual = escalonador.poll();

            double deltaTempo = eventoAtual.time - tempoGlobal;
            tempoGlobal = eventoAtual.time;

            // Atualiza os tempos acumulados de todas as filas
            for (Fila f : filas.values()) {
                f.atualizarTempo(deltaTempo);
            }

            if (eventoAtual.type == EventType.CHEGADA) {
                tratarChegada(eventoAtual.queueId);
            } else if (eventoAtual.type == EventType.SAIDA) {
                tratarSaida(eventoAtual.queueId);
            }
        }

        imprimirResultadosT1();
    }

    private static void tratarChegada(int queueId) {
        Fila fila = filas.get(queueId);

        // Se for chegada na Fila 1 (vinda de fora do sistema), agenda a próxima chegada geral
        if (queueId == 1) {
            double randomVal = rnd.nextDouble();
            if (randomVal != -1) {
                double tempoChegada = 2.0 + (4.0 - 2.0) * randomVal; // Chegadas 2..4
                escalonador.add(new Event(EventType.CHEGADA, tempoGlobal + tempoChegada, 1));
            }
        }

        // Verifica capacidade
        if (fila.estadoAtual < fila.capacidade) {
            fila.estadoAtual++;
            // Se houver servidor ocioso (estado <= servidores), agenda saída
            if (fila.estadoAtual <= fila.servidores) {
                agendarSaida(fila);
            }
        } else {
            fila.perdas++;
        }
    }

    private static void tratarSaida(int queueId) {
        Fila filaOrigem = filas.get(queueId);
        filaOrigem.estadoAtual--;

        // Se ainda tem cliente aguardando na fila de origem, agenda a próxima saída
        if (filaOrigem.estadoAtual >= filaOrigem.servidores) {
            agendarSaida(filaOrigem);
        }

        // Roteamento
        double randomRouting = rnd.nextDouble();
        if (randomRouting == -1) return;

        int destino = rotearCliente(queueId, randomRouting);

        // Se destino != 0, o cliente vai para outra fila (transferência tratada como nova chegada no tempo atual)
        if (destino != 0) {
            // Em uma transferência direta, a chegada na próxima fila ocorre no instante atual
            Fila filaDestino = filas.get(destino);
            if (filaDestino.estadoAtual < filaDestino.capacidade) {
                filaDestino.estadoAtual++;
                if (filaDestino.estadoAtual <= filaDestino.servidores) {
                    agendarSaida(filaDestino);
                }
            } else {
                filaDestino.perdas++;
            }
        }
    }

    private static void agendarSaida(Fila fila) {
        double randomVal = rnd.nextDouble();
        if (randomVal != -1) {
            double tempoAtendimento = fila.minAtendimento + (fila.maxAtendimento - fila.minAtendimento) * randomVal;
            escalonador.add(new Event(EventType.SAIDA, tempoGlobal + tempoAtendimento, fila.id));
        }
    }

    // Regras de Roteamento baseadas na imagem do T1
    private static int rotearCliente(int origem, double p) {
        if (origem == 1) {
            if (p < 0.8) return 2;
            else return 3;
        } else if (origem == 2) {
            if (p < 0.3) return 1;
            else if (p < 0.8) return 3; // 0.3 + 0.5 = 0.8
            else return 0; // 0.2 Saída do sistema
        } else if (origem == 3) {
            if (p < 0.7) return 2;
            else return 0; // 0.3 Saída do sistema
        }
        return 0;
    }

    private static void imprimirResultadosT1() {
        System.out.println("Tempo global de simulação: " + String.format("%.4f", tempoGlobal) + " minutos\n");

        for (int i = 1; i <= 3; i++) {
            Fila f = filas.get(i);
            System.out.println("Resultado da Fila " + i + ":");
            for (int estado = 0; estado <= f.capacidade; estado++) {
                // Previne imprimir milhares de estados não atingidos na fila 1 de capacidade infinita
                if (f.tempoAcumulado[estado] == 0 && estado > 15) break;

                double probabilidade = (f.tempoAcumulado[estado] / tempoGlobal) * 100;
                System.out.printf("  Estado %d: Tempo = %.4f min (%.2f%%)\n", estado, f.tempoAcumulado[estado], probabilidade);
            }
            System.out.println("  Perdas: " + f.perdas + " clientes\n");
        }
        System.out.println("dados para preenchimento do arquivo docx.");
    }

    private static void iniciarModoUsuario() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Deseja rodar um teste personalizado em uma fila G/G/1? (S/N)");
            String resp = scanner.nextLine().trim().toUpperCase();
            if (resp.equals("N")) {
                System.out.println("Encerrando...");
                break;
            } else if (resp.equals("S")) {
                try {
                    System.out.print("Min Chegada: "); double minC = Double.parseDouble(scanner.nextLine());
                    System.out.print("Max Chegada: "); double maxC = Double.parseDouble(scanner.nextLine());
                    System.out.print("Min Atendimento: "); double minA = Double.parseDouble(scanner.nextLine());
                    System.out.print("Max Atendimento: "); double maxA = Double.parseDouble(scanner.nextLine());
                    System.out.print("Capacidade (0 para infinito): "); int cap = Integer.parseInt(scanner.nextLine());
                    System.out.print("Limite de Aleatórios (ex: 10000): "); int limit = Integer.parseInt(scanner.nextLine());

                    if(cap == 0) cap = Integer.MAX_VALUE;

                    // Reinicia simulação personalizada simples de 1 fila
                    rnd = new RandomGenerator(limit);
                    escalonador = new PriorityQueue<>();
                    tempoGlobal = 0.0;
                    filas = new HashMap<>();
                    filas.put(1, new Fila(1, 1, cap, minA, maxA));

                    // Inicia gerador customizado
                    double tempoInicial = minC + (maxC - minC) * rnd.nextDouble();
                    escalonador.add(new Event(EventType.CHEGADA, tempoInicial, 1));

                    while (!escalonador.isEmpty() && !rnd.limitReached()) {
                        Event evt = escalonador.poll();
                        double delta = evt.time - tempoGlobal;
                        tempoGlobal = evt.time;
                        filas.get(1).atualizarTempo(delta);

                        if (evt.type == EventType.CHEGADA) {
                            Fila f = filas.get(1);
                            double randVal = rnd.nextDouble();
                            if (randVal != -1) {
                                double novaChegada = minC + (maxC - minC) * randVal;
                                escalonador.add(new Event(EventType.CHEGADA, tempoGlobal + novaChegada, 1));
                            }
                            if (f.estadoAtual < f.capacidade) {
                                f.estadoAtual++;
                                if (f.estadoAtual <= f.servidores) {
                                    double rvA = rnd.nextDouble();
                                    if(rvA != -1){
                                        double tempoAtend = f.minAtendimento + (f.maxAtendimento - f.minAtendimento) * rvA;
                                        escalonador.add(new Event(EventType.SAIDA, tempoGlobal + tempoAtend, 1));
                                    }
                                }
                            } else {
                                f.perdas++;
                            }
                        } else {
                            Fila f = filas.get(1);
                            f.estadoAtual--;
                            if (f.estadoAtual >= f.servidores) {
                                double rvA = rnd.nextDouble();
                                if(rvA != -1){
                                    double tempoAtend = f.minAtendimento + (f.maxAtendimento - f.minAtendimento) * rvA;
                                    escalonador.add(new Event(EventType.SAIDA, tempoGlobal + tempoAtend, 1));
                                }
                            }
                        }
                    }

                    System.out.println("\n[RESULTADO TESTE PERSONALIZADO]");
                    System.out.println("Tempo global: " + String.format("%.4f", tempoGlobal));
                    for (int estado = 0; estado <= filas.get(1).capacidade; estado++) {
                        if (filas.get(1).tempoAcumulado[estado] == 0 && estado > 20) break;
                        double prob = (filas.get(1).tempoAcumulado[estado] / tempoGlobal) * 100;
                        System.out.printf(" Estado %d: %.2f%%\n", estado, prob);
                    }
                    System.out.println(" Perdas: " + filas.get(1).perdas + "\n");

                } catch (Exception e) {
                    System.out.println("Entrada inválida. Tente novamente.");
                }
            }
        }
        scanner.close();
    }
}