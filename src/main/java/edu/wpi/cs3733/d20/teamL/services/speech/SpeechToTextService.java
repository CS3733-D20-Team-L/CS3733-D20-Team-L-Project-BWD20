package edu.wpi.cs3733.d20.teamL.services.speech;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

import edu.wpi.cs3733.d20.teamL.services.Service;
import edu.wpi.cs3733.d20.teamL.util.AsyncTaskManager;

@Slf4j
public class SpeechToTextService extends Service implements ISpeechToTextService {
	private SpeechClient client;
	private SpeechFileManager speechFileManager;
	private boolean allowStartRecording = false;
	private boolean allowDestRecording = false;

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

	@Override
	public void createClient() {
		speechFileManager = new SpeechFileManager();
		try {
			SpeechSettings settings =  SpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(speechFileManager.getCredentials())).build();
			client = SpeechClient.create(settings);
		} catch (IOException ex) {
			log.error("Encountered IOException.", ex);
		}
	}

	@Override
	public String recordAndConvertAsync(String type) {
		ForkJoinTask<Object> recordingTask = AsyncTaskManager.newTask(() -> recordSpeech(type));
		recordingTask.join();
		return convertSpeechToText();
	}

	@Override
	public String convertSpeechToText() {
		StringBuilder text = new StringBuilder();
		try {
			Path path = Paths.get(speechFileManager.getSpeechFileURI(SpeechFileManager.SpeechServiceType.SPEECH_TO_TEXT));
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);
			RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(RecognitionConfig.AudioEncoding.LINEAR16).setSampleRateHertz(24000).setLanguageCode("en-US").build();
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
			RecognizeResponse response = client.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();
			for (SpeechRecognitionResult result : results) {
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				String transcription = alternative.getTranscript();
				text.append(transcription);
				System.out.println(transcription);
			}
		} catch (IOException ex) {
			log.error("Encountered IOException.", ex);
		} catch (URISyntaxException ex) {
			log.error("Encountered URISyntaxException", ex);
		}
		return text.toString();
	}

	@Override
	public void recordSpeech(String type) {
		AudioFormat format = new AudioFormat(24000, 16, 1, true, false);
		ByteArrayOutputStream recording = new ByteArrayOutputStream();
		try {
			TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
			microphone.open(format);
			int numBytesRead;
			byte[] data = new byte[microphone.getBufferSize() / 5];
			microphone.start();
			if (type.equals("start")) {
				while (allowStartRecording()) {
					numBytesRead = microphone.read(data, 0, 1024);
					recording.write(data, 0, numBytesRead);
				}
			} else if (type.equals("dest")) {
				while (allowDestRecording()) {
					numBytesRead = microphone.read(data, 0, 1024);
					recording.write(data, 0, numBytesRead);
				}
			}
			byte[] audioData = recording.toByteArray();
			AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize());
			AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File(speechFileManager.getSpeechFileURI(SpeechFileManager.SpeechServiceType.SPEECH_TO_TEXT)));
			microphone.close();
		} catch (LineUnavailableException ex) {
			log.error("Encountered LineUnavailableException.", ex);
		} catch (URISyntaxException ex) {
			log.error("Encountered URISyntaxException.", ex);
		} catch (IOException ex) {
			log.error("Encountered IOException.", ex);
		}
	}

	@Override
	public boolean allowStartRecording() {
		return allowStartRecording;
	}

	@Override
	public void setAllowStartRecording(boolean allowStartRecording) {
		this.allowStartRecording = allowStartRecording;
	}

	@Override
	public boolean allowDestRecording() {
		return allowDestRecording;
	}

	@Override
	public void setAllowDestRecording(boolean allowDestRecording) {
		this.allowDestRecording = allowDestRecording;
	}
}
