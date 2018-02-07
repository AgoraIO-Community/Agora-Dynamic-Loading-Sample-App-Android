package agora.io.dynamicload;

import android.support.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadUtils {
    private final static String TAG = DownloadUtils.class.getSimpleName();
    private OkHttpClient mHttpClient;
    private boolean isDownloadSuccess = false;
    private OnDownloadListener l;

    public interface OnDownloadListener {
        void onDownloadSuccess(String path);

        void onDownloadProgress(int percentage);

        void onDownloadFailed(String errorMsg);
    }

    public DownloadUtils(OnDownloadListener l) {
        this.l = l;
        mHttpClient = new OkHttpClient();
    }

    /**
     * Download file from url and to saveDir
     *
     * @param url     download url
     * @param saveDir save path
     * @return isDownload success
     */
    public boolean execute(final String url, final String saveDir) {
        if (!isValidDir(saveDir) || !isValidUrl(url))
            return false;

        Request request = new Request.Builder().url(url).build();
        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (l != null)
                    l.onDownloadFailed(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                FileOutputStream fos = null;
                File saveF = null;
                byte[] buf = new byte[2048];
                int len;
                int lastPre = 0;
                try {
                    ResponseBody body = response.body();
                    if (body != null) {
                        is = body.byteStream();
                        long total = body.contentLength();
                        int current = 0;

                        saveF = new File(saveDir, getCompressFileName(url));
                        if (saveF.exists())
                            saveF.delete();

                        fos = new FileOutputStream(saveF);
                        while ((len = is.read(buf, 0, buf.length)) > 0) {
                            fos.write(buf, 0, len);
                            current += len;
                            int p = (int) (current * 1.0f / total * 100);
                            if (p - 5 > lastPre || p == 100) {
                                lastPre = p;
                                if (total != 0 && l != null)
                                    l.onDownloadProgress(lastPre);
                            }
                        }
                        fos.flush();
                        isDownloadSuccess = true;
                    } else {
                        if (l != null)
                            l.onDownloadFailed("Get response body null");
                    }
                } catch (Exception e) {
                    if (l != null) {
                        l.onDownloadFailed(e.getMessage());
                    }
                } finally {
                    if (is != null)
                        is.close();

                    if (fos != null)
                        fos.close();

                    if (isDownloadSuccess && saveF != null) {
                        decompress(saveF.getAbsolutePath(), saveDir);
                    }
                }

            }
        });
        return true;
    }

    private boolean isValidUrl(String url) {
        if (!isValidStr(url))
            return false;

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }

        return (uri.getHost() != null) && uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https");
    }

    private void decompress(String zipFile, String location) {
        try {
            FileInputStream fin = new FileInputStream(zipFile);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    isValidDir(location, ze.getName());
                } else {
                    FileOutputStream fout = new FileOutputStream(new File(location, ze.getName()));
                    BufferedOutputStream bufout = new BufferedOutputStream(fout);
                    byte[] buffer = new byte[1024];
                    int read = 0;
                    while ((read = zin.read(buffer)) != -1) {
                        bufout.write(buffer, 0, read);
                    }
                    bufout.flush();
                    bufout.close();

                    zin.closeEntry();

                    fout.flush();
                    fout.close();
                }
            }
            zin.close();
        } catch (Exception e) {
            if (l != null)
                l.onDownloadFailed("Decompress error!!!");
        } finally {
            if (l != null)
                l.onDownloadSuccess(location + File.separator + getFileName(zipFile));
        }
    }

    private boolean isValidDir(String saveDir) {
        return isValidDir(saveDir, "");
    }

    private boolean isValidDir(String location, String dir) {
        if (!isValidStr(location))
            return false;
        File f = new File(location, dir);
        if (!f.exists())
            f.mkdirs();

        return true;
    }

    private String getCompressFileName(String downloadUrl) {
        int lastIndex = downloadUrl.lastIndexOf(File.separator);
        return downloadUrl.substring(lastIndex + 1, downloadUrl.length());
    }

    private String getFileName(String downloadUrl) {
        int lastIndex = downloadUrl.lastIndexOf(File.separator);
        String zipName = downloadUrl.substring(lastIndex + 1, downloadUrl.length());
        int nameIndex = zipName.indexOf(((int) '.'));
        return zipName.substring(0, nameIndex);
    }

    private boolean isValidStr(String str) {
        return !(null == str || "".equals(str));
    }
}
