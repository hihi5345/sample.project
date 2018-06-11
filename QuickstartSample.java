// Imports the Google Cloud client library
import com.google.cloud.language.v1.*;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.snu.ids.ha.ma.MExpression;
import org.snu.ids.ha.ma.MorphemeAnalyzer;


import javax.management.timer.Timer;
import javax.print.DocFlavor;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class QuickstartSample {

    //private static String text = "나쁘지는 않지만 그렇게 감동적인 영화는 아니었다";
    //private static String text = "오늘 기분이 썩 나쁘지는 않다";
    //private static String text = "가나다라마바사";
    //private static String text = "가슴 아픈게 기분이 별로 좋지않았다";
    private static String text= "";
    private static String url;

    private static ArrayList<Integer> hti = new ArrayList<>();
    private static ArrayList<String> input = new ArrayList<>();
    private static ArrayList<String> pos = new ArrayList<>();

    private static ArrayList<String> word = new ArrayList<>();
    private static ArrayList<Integer> idx = new ArrayList<>();

    private static int sampleRateHerts = 16000;

    public static Sentiment analyzingSentiment(String text) throws Exception{
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Type.PLAIN_TEXT)
                    .build();
            AnalyzeSentimentResponse response = language.analyzeSentiment(doc);
            Sentiment sentiment = response.getDocumentSentiment();
            if (sentiment == null) {
                System.out.println("No sentiment found");
            } else {
                System.out.printf("Sentiment magnitude: %.3f\n", sentiment.getMagnitude());
                System.out.printf("Sentiment score: %.3f\n", sentiment.getScore());
            }
            return sentiment;
        }
    }

    public static List<Token> analyzingSyntax(String text) throws Exception{
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Type.PLAIN_TEXT)
                    .build();
            AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF16)
                    .build();
            // analyze the syntax in the given text
            AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
            // print the response

            for (Token token : response.getTokensList()) {
                System.out.printf("\tText: %s\n", token.getText().getContent());
                System.out.printf("\tBeginOffset: %d\n", token.getText().getBeginOffset());
                System.out.printf("Lemma: %s\n", token.getLemma());
                System.out.printf("PartOfSpeechTag: %s\n", token.getPartOfSpeech().getTag());

                System.out.println("DependencyEdge");
                System.out.printf("\tHeadTokenIndex: %d\n", token.getDependencyEdge().getHeadTokenIndex());
                System.out.printf("\tLabel: %s\n\n", token.getDependencyEdge().getLabel());

                hti.add(token.getDependencyEdge().getHeadTokenIndex());
                input.add(token.getText().getContent());
                word.add(token.getText().getContent());
                idx.add(token.getDependencyEdge().getHeadTokenIndex());
                pos.add(token.getDependencyEdge().getLabel().toString());
                if(token.getPartOfSpeech().getTag().toString().equals("PRT") || token.getPartOfSpeech().getTag().toString().equals("AFFIX")){
                    if(word.size()>1) {
                        String temp = word.get(word.size() - 2) + word.get(word.size() - 1);
                        word.remove(word.size() - 1);
                        word.remove(word.size() - 1);
                        word.add(temp);
                        word.add("ㄲ");
                        //idx.remove(idx.size() - 1);
                    }
                }
             }
            return response.getTokensList();
        }
    }

    private static AudioInputStream convertChannels(
            int nChannels,
            AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
                sourceFormat.getEncoding(),
                sourceFormat.getSampleRate(),
                sourceFormat.getSampleSizeInBits(),
                nChannels,
                calculateFrameSize(nChannels,
                        sourceFormat.getSampleSizeInBits()),
                sourceFormat.getFrameRate(),
                sourceFormat.isBigEndian());
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }
    private static int calculateFrameSize(int nChannels, int nSampleSizeInBits)
    {
        return ((nSampleSizeInBits + 7) / 8) * nChannels;
    }

public static void main(String... args) throws Exception {

    ServerSocket Server = new ServerSocket(5000);
    Socket connected;

    while (true) {

        hti.clear();
        input.clear();
        pos.clear();

        word.clear();
        idx.clear();

        System.out.println("TCPServer Waiting for client on port 5000");
        connected = Server.accept();
        BufferedReader br = new BufferedReader(new InputStreamReader(connected.getInputStream()));
        String str;
        PrintWriter pw = new PrintWriter(connected.getOutputStream(), true);

        while ((str = br.readLine()) != null) {
            System.out.println("The message: " + str);
            url = str;

            break;
        }


        ArrayList<String> output = new ArrayList<>();
        ArrayList<Integer> outputIdx = new ArrayList<>();


        ArrayList<String> neg = new ArrayList<>();
        neg.add("안");
        neg.add("못");
        neg.add("않");
        neg.add("없");
        neg.add("아니");


        DBManager dbManager = new DBManager();
        dbManager.connect();

        //////////////////////////////////////////////////////////////
        InputStream streamAudio = new URL(url).openStream();
        InputStream bufferedIn = new BufferedInputStream(streamAudio);
        AudioInputStream inputAIS = AudioSystem.getAudioInputStream(bufferedIn);
        sampleRateHerts = (int) inputAIS.getFormat().getSampleRate();
        if(inputAIS.getFormat().getChannels() == 2){
            System.out.println("this is stereo file");
            inputAIS = convertChannels(1, inputAIS);

            Files.copy(inputAIS, Paths.get("resources/check" + ".wav"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("converting success");
        } else {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, Paths.get("resources/check" + ".wav"), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("success");

            }
        }



        ///////////////////////////////////////////////////////////////

        try (SpeechClient speechClient = SpeechClient.create()) {



            // The path to the audio file to transcribe
            String fileName = "resources/check.wav";
            // Reads the audio file into memory
            //Path path = Paths.get(fileName);
            Path path = Paths.get(fileName);



            byte[] data = Files.readAllBytes(path);
            ByteString audioBytes = ByteString.copyFrom(data);

            // Builds the sync recognize request
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(sampleRateHerts)
                    .setLanguageCode("ko-KR")
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // Performs speech recognition on the audio file
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            for (SpeechRecognitionResult result : results) {
                // There can be several alternative transcripts for a given chunk of speech. Just use the
                // first (most likely) one here.
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                System.out.printf("Transcription: %s%n", alternative.getTranscript());
                text = alternative.getTranscript();
            }
        }

        System.out.println("Sentiment Anayzing.");
        analyzingSentiment(text);
        System.out.println("Syntax Anayzing.");
        analyzingSyntax(text);

        /////어간추출 테스트
        MorphemeAnalyzer ma = new MorphemeAnalyzer();
        ma.createLogger(null);
        Timer timer = new Timer();
        timer.start();


        ///////////////test////////////////
        // for(int a=0;a<input.size();a++) {
        ///////////////test////////////////

        int negCheck = 0;
        //List<MExpression> ret = ma.analyze(input.get(a));
        for (int textIndex = 0; textIndex < word.size(); textIndex++) {
            List<MExpression> ret = ma.analyze(word.get(textIndex));
            timer.stop();
            ret = ma.postProcess(ret);
            ret = ma.leaveJustBest(ret);
            List<org.snu.ids.ha.ma.Sentence> stl = ma.divideToSentences(ret);
            for (int i = 0; i < stl.size(); i++) {
                org.snu.ids.ha.ma.Sentence st = stl.get(i);
                for (int j = 0; j < st.size(); j++) {
                    //System.out.println(st.get(j));

                    String[] temp = st.get(j).toString().split("\\+");
                    for (int k = 0; k < temp.length; k++) {
                        String[] temp1 = temp[k].split("/");
                        output.add(temp1[1]);
                        output.add(temp1[2].replace("]", ""));
                        outputIdx.add(textIndex + negCheck);
                        outputIdx.add(textIndex + negCheck);
                        /*
                        if (!(dbManager.checkStopWord(output.get(output.size() - 2), output.get(output.size() - 1))) && !(neg.contains(temp1[1]))) {
                            negCheck++;
                            output.remove(output.size() - 1);
                            output.remove(output.size() - 1);
                            outputIdx.remove(outputIdx.size() - 1);
                            outputIdx.remove(outputIdx.size() - 1);
                        }
                        */
                    }
                }

            }
        }

        for(int i=0;i<output.size();i=i+2){
            if (!(dbManager.checkStopWord(output.get(i), output.get(i + 1))) && !(neg.contains(output.get(i)))) {
                output.remove(i);
                output.remove(i);
                outputIdx.remove(i);
                outputIdx.remove(i);
            }
        }










/*
        for(int i=0;i<output.size();i++){
            System.out.println(output.get(i));
        }
*/


        System.out.println("\n");
        for (int i = 0; i < output.size(); i++) {
            System.out.println(output.get(i) + " " + outputIdx.get(i));
        }

        System.out.println("\n\nheadtokenindex\n");
        for (int i = 0; i < idx.size(); i++) {
            System.out.print(word.get(i) + " ");
            System.out.println(idx.get(i));
        }

        System.out.println("");
        for (int i = 0; i < input.size(); i++) {
            System.out.println(input.get(i));
        }

        System.out.println(text);
        System.out.println("Sentimental Score(-1 ~ 1) : " + dbManager.extractEmotion(output, outputIdx, input, hti, pos));
        double op = dbManager.extractEmotion(output, outputIdx, input, hti, pos);

        pw.println(String.valueOf(op));

        connected.close();

        ma.closeLogger();

        dbManager.doFinal();

    }
}
}