package com.example.multivideos;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamDataSourceFactory implements DataSource.Factory {

    private InputStream inputStream;

    public InputStreamDataSourceFactory(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public DataSource createDataSource() {
        return new InputStreamDataSource(inputStream);
    }

    private static class InputStreamDataSource implements DataSource {

        private InputStream inputStream;

        public InputStreamDataSource(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void addTransferListener(TransferListener transferListener) {

        }

        @Override
        public long open(DataSpec dataSpec) throws IOException {
            return inputStream.available();
        }

        @Nullable
        @Override
        public Uri getUri() {
            return null;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public int read(byte[] buffer, int offset, int readLength) throws IOException {
            return inputStream.read(buffer, offset, readLength);
        }
    }
}

