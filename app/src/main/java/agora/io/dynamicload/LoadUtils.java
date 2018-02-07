package agora.io.dynamicload;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

public class LoadUtils extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = LoadUtils.class.getSimpleName();
    private WeakReference<Context> mContext;
    private OnSoLoadBeReady mOnLoad;

    public LoadUtils(WeakReference<Context> contextWeakReference, OnSoLoadBeReady ready) {
        if (contextWeakReference == null)
            throw new NullPointerException("contextWeakReference should not be null");

        this.mContext = contextWeakReference;
        this.mOnLoad = ready;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        if (!isValidStr(strings[0]) || !isValidStr(strings[1]))
            return null;

        String loadPath;
        if ((loadPath = addToSearchPath(strings[0])) == null) {
            return false;
        } else {
            System.load(loadPath + File.separator + strings[1]);
            return true;
        }
    }

    @Override
    protected void onPostExecute(Boolean s) {
        super.onPostExecute(s);
        if (mOnLoad != null)
            mOnLoad.onLoadReady(s);
    }

    public interface OnSoLoadBeReady {
        void onLoadReady(Boolean isLoadSuccess);
    }


    /**
     * dynamic load so with add customer path to system search lists
     * this app ust adapted dalvik.system.BaseDexClassLoader
     * if you want to adapted ali-os, just adapted dalvik.system.LexClassLoader
     */
    public String addToSearchPath(String srcPath) {
        if (!isValidStr(srcPath) || mContext.get() == null)
            return null;

        String loadPath = null;
        File des = mContext.get().getDir("jniLibs", Context.MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 21) {
            String[] abis = Build.SUPPORTED_ABIS;
            for (String abi : abis) {
                File desAbi = new File(des, abi);
                File src = new File(srcPath, abi);
                if (src.exists()) {
                    copyToDes(src, desAbi);
                    loadPath = desAbi.getAbsolutePath();
                    break;
                }
            }
        } else {
            String abi = Build.CPU_ABI;
            File desAbi = new File(des, abi);
            File src = new File(srcPath, abi);
            if (!src.exists() && Build.CPU_ABI2 != null) {
                src = new File(srcPath, Build.CPU_ABI2);
                if (!src.exists()) {
                    src = new File(srcPath, "armeabi-v7a");
                    if (src.exists()) {
                        copyToDes(src, desAbi);
                    }
                }
            }

            copyToDes(src, desAbi);
            loadPath = desAbi.getAbsolutePath();
        }

        return initNativeDirectory(loadPath);
    }

    private void copyToDes(File src, File dest) {
        if (src.isDirectory()) {
            if (!dest.exists())
                dest.mkdirs();

            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyToDes(srcFile, destFile);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(src);
                out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];
                int length;

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null)
                        in.close();
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isValidStr(String str) {
        return !(null == str || "".equals(str));
    }

    private String initNativeDirectory(String path) {
        String retPath = null;

        if (!isValidStr(path))
            return null;

        if (hasDexClassLoader()) {
            try {
                retPath = createNewNativeDir(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return retPath;
    }

    private boolean hasDexClassLoader() {
        try {
            Class.forName("dalvik.system.BaseDexClassLoader");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private String createNewNativeDir(String path) throws Exception {
        if (mContext.get() == null)
            return null;

        String createPath = null;

        PathClassLoader pathClassLoader = (PathClassLoader) mContext.get().getClassLoader();
        Field declaredField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList");
        declaredField.setAccessible(true);
        Object dexPathList = declaredField.get(pathClassLoader);

        // API < M & API >= M findLibrary while search diff path, see code at
        // /libcore/dalvik/src/main/java/dalvik/system/DexPathList.java
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Object nativeLibraryDirectories = dexPathList.getClass().getDeclaredField("nativeLibraryDirectories");
            ((Field) nativeLibraryDirectories).setAccessible(true);

            File[] files = (File[]) ((Field) nativeLibraryDirectories).get(dexPathList);
            Object filesss = Array.newInstance(File.class, files.length + 1);
            Array.set(filesss, 0, new File(path));
            for (int i = 1; i < files.length + 1; i++) {
                Array.set(filesss, i, files[i - 1]);
            }
            ((Field) nativeLibraryDirectories).set(dexPathList, filesss);

            createPath = path;
        } else {
            Class<?>[] innerClz = Class.forName("dalvik.system.DexPathList").getDeclaredClasses();
            Class eleClz = null;
            for (Class clz : innerClz) {
                if (clz.getSimpleName().contains("Element")) {
                    eleClz = clz;
                    break;
                }
            }

            Constructor ele;
            if (eleClz != null) {
                Class<?>[] params = {File.class, Boolean.TYPE, File.class, DexFile.class};
                ele = eleClz.getDeclaredConstructor(params);
                ele.setAccessible(true);

                Object nativeLibraryPathElements = dexPathList.getClass().getDeclaredField("nativeLibraryPathElements");
                ((Field) nativeLibraryPathElements).setAccessible(true);

                Object[] objs = (Object[]) ((Field) nativeLibraryPathElements).get(dexPathList);
                Object ob = ele.newInstance(new File(path), true, null, null);
                Object eles = Array.newInstance(eleClz, objs.length + 1);
                Array.set(eles, 0, ob);
                for (int i = 1; i < objs.length + 1; i++) {
                    Array.set(eles, i, objs[i - 1]);
                }
                ((Field) nativeLibraryPathElements).set(dexPathList, eles);

                createPath = path;
            }
        }

        return createPath;
    }
}
