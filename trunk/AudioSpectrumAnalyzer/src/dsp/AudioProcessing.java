package dsp;

import dsp.SignalHelper.SignalGenerator;
import fft.Constants;
import fft.FFTHelper;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioProcessing extends Thread {

    private static final String TAG = AudioProcessing.class.getSimpleName();

    private double mSampleRateInHz;
    private int mNumberOfFFTPoints;

    private AudioRecord mRecorder;
    private int mMinBufferSize;

    private boolean stopped;

    private static AudioProcessingListener mListener;

    private FFTHelper mFFT;

    public AudioProcessing(double sampleRate, int numberOfFFTPoints) {
        mSampleRateInHz = sampleRate;
        mNumberOfFFTPoints = numberOfFFTPoints;
        mFFT = new FFTHelper(mSampleRateInHz, mNumberOfFFTPoints);
        start();
    }

    @Override
    public void run() {
        if (Constants.DEBUG_MODE) {
            runWithSignalHelper();
        } else {
            runWithAudioRecord();
        }
    }

    private void runWithSignalHelper() { // TESTE
        int numberOfReadBytes = 0, bufferSize = 2 * mNumberOfFFTPoints;
        double[] absNormalizedSignal;

        while (!stopped) {
            byte tempBuffer[] = new byte[bufferSize]; // 2*Buffer size because
                                                      // it's a short variable
                                                      // into a array of bytes.
            numberOfReadBytes = SignalGenerator.read(tempBuffer, 100,
                    mNumberOfFFTPoints, mSampleRateInHz, true, false);
            if (numberOfReadBytes > 0) {
                if (mFFT != null) {
                    // Calculate captured signal's FFT.
                    absNormalizedSignal = mFFT.calculateFFT(tempBuffer);
                    notifyListenersOnFFTSamplesAvailableForDrawing(absNormalizedSignal);
                }
            } else {
                Log.e(TAG,
                        "There was an error reading the audio device - ERROR: "
                                + numberOfReadBytes);
            }
        }
    }

    private void runWithAudioRecord() {
        int numberOfReadBytes = 0, bufferSize = 2 * mNumberOfFFTPoints;
        double[] absNormalizedSignal;

        mMinBufferSize = AudioRecord.getMinBufferSize((int) mSampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                (int) mSampleRateInHz, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 10 * mMinBufferSize);

        if (mRecorder == null)
            return;

        mRecorder.startRecording();

        while (!stopped) {
            byte tempBuffer[] = new byte[bufferSize];
            numberOfReadBytes = mRecorder.read(tempBuffer, 0, bufferSize);
            if (numberOfReadBytes > 0) {
                if (mFFT != null) {
                    // Calculate captured signal's FFT.
                    absNormalizedSignal = mFFT.calculateFFT(tempBuffer);
                    notifyListenersOnFFTSamplesAvailableForDrawing(absNormalizedSignal);
                }
            } else {
                Log.e(TAG,
                        "There was an error reading the audio device - ERROR: "
                                + numberOfReadBytes);
            }
        }

        mRecorder.stop();
        mRecorder.release();
    }

    public double getPeakFrequency() {
        return mFFT.getPeakFrequency();
    }

    public double getPeakFrequency(int[] absSignal) {
        return mFFT.getPeakFrequency(absSignal);
    }

    public double getMaxFFTSample() {
        return mFFT.getMaxFFTSample();
    }

    public void close() {
        stopped = true;
    }

    public static void registerDrawableFFTSamplesAvailableListener(
            AudioProcessingListener listener) {
        mListener = listener;
    }

    public static void unregisterDrawableFFTSamplesAvailableListener() {
        mListener = null;
    }

    public void notifyListenersOnFFTSamplesAvailableForDrawing(
            double[] absSignal) {
        if (mListener != null)
            mListener.onDrawableFFTSignalAvailable(absSignal);
    }
}