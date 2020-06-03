package chatbot;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;

import java.io.IOException;
import javafx.scene.image.ImageView;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import java.io.FileNotFoundException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import javax.sound.sampled.LineUnavailableException;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.util.WaveUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The ChatBotController class defines the method in the fxml file, including
 * sending messages, transferring voice to text, and transferring text to voice
 * using IBM Waston API.
 *
 * @author Group 8
 */
public class ChatBotController {

    @FXML
    private Button sendMessagesBtn;
    @FXML
    private TextField message;
    @FXML
    private TextArea showTxtLabel;
    @FXML
    private ImageView read;
    @FXML
    private ImageView record;

    String path = "E:/NetBeans Projects/ChatBot/src/resources";
    String name = "admission";

    Bot bot = new Bot(name, path);
    Chat chatSession = new Chat(bot);

    public String val2;

    /**
     * The sendMessages method reacts to the "send" button click and sends the
     * message to the screen.
     */
    @FXML
    private void sendMessages() {
        String val1 = message.getText(); //get the text entered in the textfield
        val2 = val1;
        if (val1.equals("")) {
            System.out.println("Cannot send empty message!");
        } else {
            String response1 = chatSession.multisentenceRespond(val1);
            showTxtLabel.setText(showTxtLabel.getText() + "\nMe: " + val1 + "\nBot: " + response1);
            showTxtLabel.positionCaret(showTxtLabel.getText().length());
            message.clear();
        }
    }

    /**
     * The voiceRecord method transfers the user's voice into text using IBM
     * Waston API
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws LineUnavailableException
     */
    @FXML
    public void voiceRecord() throws IOException, InterruptedException, LineUnavailableException {
        // set the api
        IamOptions option = new IamOptions.Builder()
                .apiKey("BiS170ZCgFKzDLmO6L0oglcbQPK2XoLNxexrwkgEkeYP")
                .build();
        SpeechToText service = new SpeechToText(option);
        service.setEndPoint("https://gateway-syd.watsonplatform.net/speech-to-text/api");

        // Signed PCM AudioFormat with 16kHz, 16 bit sample size, mono
        int sampleRate = 16000;
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
            System.exit(0);
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        AudioInputStream audio = new AudioInputStream(line);

        RecognizeOptions options = new RecognizeOptions.Builder()
                .audio(audio)
                .interimResults(true)
                .timestamps(true)
                .wordConfidence(true)
                .inactivityTimeout(5) // use this to stop listening when the speaker pauses for 5s
                .contentType(HttpMediaType.AUDIO_RAW + ";rate=" + sampleRate)
                .build();

        service.recognizeUsingWebSocket(options, new BaseRecognizeCallback() {
            @Override
            public void onTranscription(SpeechRecognitionResults speechResults) {
                message.setText(speechResults.getResults().get(0).getAlternatives().get(0).getTranscript());
            }
        });

        System.out.println("Listening to your voice for the next 20s...");
        Thread.sleep(20 * 1000);

        // closing the WebSockets underlying InputStream will close the WebSocket itself.
        line.stop();
        line.close();

        System.out.println("End.");

        // from the file
        /*
    File audio = new File("E:\\CMUA sem1\\JAVA\\group project\\t3.wav");
    RecognizeOptions options = new RecognizeOptions.Builder()
        .audio(audio)
        .contentType(RecognizeOptions.ContentType.AUDIO_WAV)
        .build();
    SpeechRecognitionResults transcript = service.recognize(options).execute();

    System.out.println(transcript);
         */
    }

    /**
     * The readMessage method read the chatbot responses out (transfer the text
     * to voice) through the use of IBM Waston API.
     */
    @FXML
    public void readMessage() {
        // set the api
        IamOptions options = new IamOptions.Builder()
                .apiKey("SzlOF9aao4ZXYynIVo-s3UxWxf5IMi6CFu9DgBue3jZz")
                .build();

        TextToSpeech textToSpeech = new TextToSpeech(options);

        textToSpeech.setEndPoint("https://stream.watsonplatform.net/text-to-speech/api");

        String text = chatSession.multisentenceRespond(val2);
        try {
            SynthesizeOptions synthesizeOptions
                    = new SynthesizeOptions.Builder()
                            .text(text)
                            .accept("audio/wav")
                            .voice("en-US_AllisonVoice")
                            .build();

            InputStream inputStream = textToSpeech.synthesize(synthesizeOptions).execute();
            InputStream in = WaveUtils.reWriteWaveHeader(inputStream);

            OutputStream out = new FileOutputStream("response.wav");
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            out.close();
            in.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AePlayWave aw = new AePlayWave("response.wav");
        aw.start();
    }

    /**
     * The initialize method initializes the controller class.
     *
     * @param url
     * @param rb
     * @throws IOException
     */
    public void initialize(URL url, ResourceBundle rb) throws IOException {

        // voiceRecord(recognizer);
    }
}
