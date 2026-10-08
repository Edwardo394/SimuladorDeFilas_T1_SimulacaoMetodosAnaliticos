Simulador de Rede de Filas - T1
Este projeto é um simulador de eventos discretos em Java feito para a disciplina de Simulação e Métodos Analíticos. Ele roda automaticamente o cenário de teste da atividade T1 e depois permite testar novos valores de filas.
Requisitos
* Java JDK 8 ou superior instalado.
Como Compilar e Rodar
1. Abra o terminal na pasta onde está o arquivo SimuladorRedeFilas.java.
2. Compile o código com o comando:
javac SimuladorRedeFilas.java

3. Execute o programa:
java SimuladorRedeFilas

Como Usar
   1. Simulação Automática (T1):
Assim que você roda o programa, ele executa os 100.000 números aleatórios do teste padrão e exibe os resultados na tela (tempos, porcentagens por estado e perdas).
   2. Modo Manual:
Após terminar o teste do T1, o programa vai perguntar no terminal se você quer rodar um teste personalizado.
      * Digite S para simular uma nova fila e preencha os dados pedidos (chegadas, atendimentos, capacidade e quantidade de aleatórios).
      * Digite N para fechar o programa.