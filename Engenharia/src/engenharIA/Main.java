package engenharIA;

import okhttp3.*;
import com.google.gson.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "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";

    private static List<JsonObject> chatHistory = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("EngenharIA Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);

        JTextArea responseArea = new JTextArea();
        responseArea.setEditable(false);

        // Campos para cada característica
        JTextField profundidadeField = new JTextField();
        JTextField tipoSoloField = new JTextField();
        JTextField sptField = new JTextField();
        JTextField capacidadeField = new JTextField();
        JTextField nivelField = new JTextField();
        JTextField tensaoField = new JTextField();
        JTextField cargaField = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(8, 2));
        inputPanel.add(new JLabel("1- Profundidade da Sondagem (m):")); inputPanel.add(profundidadeField);
        inputPanel.add(new JLabel("2- Tipo de solo:")); inputPanel.add(tipoSoloField);
        inputPanel.add(new JLabel("3- SPT (golpes):")); inputPanel.add(sptField);
        inputPanel.add(new JLabel("4- Capacidade de carga (kN/m2):")); inputPanel.add(capacidadeField);
        inputPanel.add(new JLabel("5- Nível do lençol freático (m):")); inputPanel.add(nivelField);
        inputPanel.add(new JLabel("6- Tensão admissível (kN/m2):")); inputPanel.add(tensaoField);
        inputPanel.add(new JLabel("7- Carga da estrutura (kN):")); inputPanel.add(cargaField);

        // Botões
        JButton btnObjetiva = new JButton("Resposta Objetiva");
        JButton btnExplicada = new JButton("Resposta Explicada");

        btnObjetiva.addActionListener((ActionEvent e) -> {
            enviarPergunta(responseArea, profundidadeField, tipoSoloField, sptField, capacidadeField, nivelField, tensaoField, cargaField);
        });

        btnExplicada.addActionListener((ActionEvent e) -> {
            enviarExplicacao(responseArea);
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnObjetiva);
        buttonPanel.add(btnExplicada);

        frame.getContentPane().add(inputPanel, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(responseArea), BorderLayout.CENTER);
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void enviarPergunta(JTextArea responseArea, JTextField profundidadeField, JTextField tipoSoloField,
                                       JTextField sptField, JTextField capacidadeField, JTextField nivelField,
                                       JTextField tensaoField, JTextField cargaField) {

        chatHistory.clear(); // Começa um novo contexto

        String pergunta = montarPergunta(
                profundidadeField.getText(),
                tipoSoloField.getText(),
                sptField.getText(),
                capacidadeField.getText(),
                nivelField.getText(),
                tensaoField.getText(),
                cargaField.getText()
        );

        responseArea.append("\n[Enviando pergunta objetiva...]\n");

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", pergunta);
        chatHistory.add(userMessage);

        new Thread(() -> {
            String assistantResponse = sendMessageToAPI(chatHistory);

            JsonObject assistantMessage = new JsonObject();
            assistantMessage.addProperty("role", "assistant");
            assistantMessage.addProperty("content", assistantResponse);
            chatHistory.add(assistantMessage);

            SwingUtilities.invokeLater(() -> responseArea.append("Assistente (Objetiva): " + assistantResponse + "\n\n"));
        }).start();
    }

    private static void enviarExplicacao(JTextArea responseArea) {
        if (chatHistory.isEmpty()) {
            responseArea.append("\n[Primeiro envie uma pergunta objetiva antes de pedir explicação.]\n");
            return;
        }

        responseArea.append("\n[Pedindo explicação detalhada...]\n");

        JsonObject explicacaoRequest = new JsonObject();
        explicacaoRequest.addProperty("role", "user");
        explicacaoRequest.addProperty("content", "Agora explique essa resposta de forma técnica e detalhada em PORTUGUÊS BR .");
        chatHistory.add(explicacaoRequest);

        new Thread(() -> {
            String assistantResponse = sendMessageToAPI(chatHistory);

            JsonObject assistantMessage = new JsonObject();
            assistantMessage.addProperty("role", "assistant");
            assistantMessage.addProperty("content", assistantResponse);
            chatHistory.add(assistantMessage);

            SwingUtilities.invokeLater(() -> responseArea.append("Assistente (Explicada): " + assistantResponse + "\n\n"));
        }).start();
    }

    private static String montarPergunta(String profundidade, String tipoSolo, String spt, String capacidade,
                                         String nivel, String tensao, String carga) {

        return "Qual melhor tipo de fundação para uma obra com as seguintes características:" +
                " 1- Profundidade da Sondagem (m) que é a profundidade máxima investigada no subsolo igual á " + profundidade + "m" +
                " 2- Tipo de solo que é a classificação da camada predominante igual á " + tipoSolo +
                " 3- SPT(Golpes) que é o número de golpes por metro nas camadas sondadas igual a " + spt +
                " 4- Capacidade de carga admissível (kN/m2) que é o valor estimado de suporte do solo igual a " + capacidade + " kN/m2" +
                " 5- Nivel do lençol freático (m) que é a profundidade em que se encontra a água subterrânea igual a " + nivel + "m" +
                " 6- Tensão admissível de serviço que é a tensão máxima que o solo pode receber igual a " + tensao + " kN/m2" +
                " 7- Carga da estrutura (kN) que é o esforço vertical total transmitido a fundação igual a " + carga + "kN. " +
                "Responda de forma direta e objetiva em PORTUGUÊS BR.";
    }

    private static String sendMessageToAPI(List<JsonObject> history) {
        try {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("model", "deepseek/deepseek-chat:free");

            JsonArray messages = new JsonArray();
            for (JsonObject msg : history) {
                messages.add(msg);
            }
            jsonBody.add("messages", messages);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");
                    JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                    return message.get("content").getAsString();
                } else {
                    return "Falha na requisição. Código: " + response.code();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Erro: " + ex.getMessage();
        }
    }
}
