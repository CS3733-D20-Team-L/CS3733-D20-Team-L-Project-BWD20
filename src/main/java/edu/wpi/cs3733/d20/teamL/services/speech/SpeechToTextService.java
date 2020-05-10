package edu.wpi.cs3733.d20.teamL.services.speech;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import edu.wpi.cs3733.d20.teamL.services.Service;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class SpeechToTextService extends Service {
	private SpeechClient client;
	private SpeechFileManager speechFileManager;

	public SpeechToTextService() {
		super();
		this.serviceName = "google-stt-01";
	}

	@Override
	public void startService() {
		createClient();
	}

	@Override
	public void stopService() {
		client.close();
	}

	public void createClient() {
		speechFileManager = new SpeechFileManager();
		try {
			SpeechSettings settings =  SpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(speechFileManager.getCredentials())).build();
			client = SpeechClient.create(settings);
		} catch (IOException ex) {
			log.error("Encountered IOException.", ex);
		}
	}

	public String convertSpeechToText() {
		StringBuilder text = new StringBuilder();
		try {
			//TODO: change to recording file
			Path path = Paths.get(speechFileManager.getSpeechFileURI(SpeechFileManager.SpeechServiceType.SPEECH_TO_TEXT));
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);
			RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(RecognitionConfig.AudioEncoding.LINEAR16).setSampleRateHertz(24000).setLanguageCode("en-US").build();
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
			RecognizeResponse response = client.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();
			for (SpeechRecognitionResult result : results) {
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				text.append(alternative.getTranscript());
			}
		} catch (IOException ex) {
			log.error("Encountered IOException.", ex);
		} catch (URISyntaxException ex) {
			log.error("Encountered URISyntaxException", ex);
		}
		return text.toString();
	}

	public void recordSpeech() {
		AudioFormat format = new AudioFormat(24000, 16, 1, true, false);
		ByteArrayOutputStream recording = new ByteArrayOutputStream();
		try {
			TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
			microphone.open(format);
			int numBytesRead;
			byte[] data = new byte[microphone.getBufferSize() / 5];
			microphone.start();
			int bytesRead = 0;
			while (bytesRead < 320000) {
				numBytesRead = microphone.read(data, 0, 1024);
				bytesRead += numBytesRead;
				System.out.println("Recording...");
				recording.write(data, 0, numBytesRead);
			}
			byte[] audioData = recording.toByteArray();
			AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(audioData), format,audioData.length / format.getFrameSize());
			AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File(speechFileManager.getSpeechFileURI(SpeechFileManager.SpeechServiceType.SPEECH_TO_TEXT)));
			microphone.close();
			String transcript = convertSpeechToText();
			System.out.println(transcript.substring(0, 1).toUpperCase() + transcript.substring(1) + ".");
		} catch (LineUnavailableException ex) {
			log.error("Encountered LineUnavailableException.", ex);
		} catch (URISyntaxException ex) {
			log.error("Encountered URISyntaxException.", ex);
		} catch (IOException ex) {
			log.error("Encountered IOException.", ex);
		}
	}
}
