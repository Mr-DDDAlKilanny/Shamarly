//
//    private Bitmap getBitmapFromURL(String link) {
//        System.out.println(link);
//        try {
//            URL url = new URL(link);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setDoInput(true);
//            if (Build.VERSION.SDK_INT > 13) // fix java.io.EOFException at libcore.io.Streams.readAsciiLine(Streams.java:203)
//                connection.setRequestProperty("Connection", "close");
//            connection.connect();
//            InputStream input = connection.getInputStream();
//            BitmapFactory.Options o = new BitmapFactory.Options();
//            return BitmapFactory.decodeStream(input, new Rect(0, 0, 0, 0), o);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private int downloadAndSavePage(int idx, byte[] buffer) {
//        int error = 1;
//        try {
//            File filename = new File(getAlbumStorageDir(getApplicationContext()), idx + "");
//            final String downloadUrl = getString(setting.downloadQuality == Setting.QUALITY_HIGH ?
//                    R.string.downloadPage_high : R.string.downloadPage_low);
//            URL url = new URL(String.format(Locale.US, downloadUrl, idx));
//            URLConnection connection = url.openConnection();
//            if (Build.VERSION.SDK_INT > 13) // fix java.io.EOFException at libcore.io.Streams.readAsciiLine(Streams.java:203)
//                connection.setRequestProperty("Connection", "close");
//            connection.connect();
//            InputStream input = new BufferedInputStream(connection.getInputStream());
//            error = 2;
//            OutputStream output = new FileOutputStream(filename);
//            int count;
//            while ((count = input.read(buffer)) != -1)
//                output.write(buffer, 0, count);
//            output.flush();
//            output.close();
//            input.close();
//            error = 0;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return error;
//    }
//
//    private void downloadAll() {
//        final ProgressDialog show = new ProgressDialog(this);
//        show.setTitle("تحميل المصحف كاملا");
//        show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        show.setIndeterminate(false);
//        final int MAX_PAGE = FullScreenImageAdapter.MAX_PAGE;
//        show.setMax(MAX_PAGE);
//        show.setProgress(0);
//        show.show();
//        Display display = getWindowManager().getDefaultDisplay();
//        final Point p = new Point();
//        display.getSize(p);
//        final AsyncTask<Void, Integer, String[]> execute = new AsyncTask<Void, Integer, String[]>() {
//            @Override
//            protected String[] doInBackground(Void... params) {
//                final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
//                int exist = 0;
//                for (int p = 1; !isCancelled() && p <= MAX_PAGE; ++p) {
//                    Bitmap bb = readPage(p);
//                    if (bb == null) {
//                        q.add(p);
//                    } else {
//                        publishProgress(++exist);
//                        bb.recycle();
//                    }
//                }
//                if (isCancelled()) return null;
//                Thread[] threads = new Thread[4];
//                final Shared progress = new Shared();
//                final Shared error = new Shared();
//                error.setData(0);
//                progress.setData(exist);
//                if (exist == 0) {
//                    final Lock lock = new ReentrantLock(true);
//                    final Condition condition = lock.newCondition();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                            builder.setTitle("حدد جودة التحميل");
//                            final Runnable signal = new Runnable() {
//                                @Override
//                                public void run() {
//                                    lock.lock();
//                                    condition.signalAll();
//                                    lock.unlock();
//                                }
//                            };
//                            builder.setItems(new String[]{"جودة عالية (مساحة أكبر)", "جودة عادية (مساحة أقل)"},
//                                    new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            if (which == 0)
//                                                setting.downloadQuality = Setting.QUALITY_HIGH;
//                                            else
//                                                setting.downloadQuality = Setting.QUALITY_LOW;
//                                            saveSettings();
//                                            signal.run();
//                                        }
//                                    });
//                            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                                @Override
//                                public void onCancel(DialogInterface dialog) {
//                                    setting.downloadQuality = Setting.QUALITY_UNSPECIFIED;
//                                    saveSettings();
//                                    signal.run();
//                                }
//                            });
//                            builder.show();
//                        }
//                    });
//                    try {
//                        lock.lock();
//                        condition.await();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    lock.unlock();
//                }
//                if (setting.downloadQuality == Setting.QUALITY_UNSPECIFIED) {
//                    cancel(false);
//                    return null; // user cancelled quality selection dialog
//                }
//                for (int th = 0; th < threads.length; ++th) {
//                    threads[th] = new Thread(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            byte[] buf = new byte[1024];
//                            while (!isCancelled() && error.getData() == 0) {
//                                Integer per = q.poll();
//                                if (per == null) break;
//                                error.setData(downloadAndSavePage(per, buf));
//                                progress.increment();
//                                publishProgress(progress.getData());
//                            }
//                        }
//                    });
//                }
//                for (int i = 0; i < threads.length; ++i)
//                    threads[i].start();
//                for (int i = 0; i < threads.length; ++i)
//                    try {
//                        threads[i].join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                if (error.getData() == 1)
//                    return new String[]{"خطأ", "فشلت عملية التحميل. تأكد من اتصالك بالانترنت"};
//                else if (error.getData() == 2)
//                    return new String[]{"خطأ", "لا يمكن كتابة الملف. تأكد من وجود مساحة كافية"};
//                else if (!isCancelled()) {
//                    return new String[]{"تحميل المصحف", "جميع الصفحات تم تحميلها بنجاح"};
//                }
//                return null;
//            }
//
//            @Override
//            protected void onProgressUpdate(final Integer... values) {
//                show.setProgress(values[0]);
//            }
//
//            @Override
//            protected void onCancelled() {
//                //super.onCancelled();
//                show.dismiss();
//            }
//
//            @Override
//            protected void onPostExecute(String[] strings) {
//                //super.onPostExecute(strings);
//                show.dismiss();
//                showAlert(strings[0], strings[1]);
//                if (strings[1] != null && strings[1].contains("نجاح"))
//                    initViewPagerAdapter();
//            }
//        }.execute();
//        show.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                execute.cancel(true);
//            }
//        });
//    }
//
//    public static void deleteAll(Context context) {
//        for (int idx = 1; idx <= FullScreenImageAdapter.MAX_PAGE; ++idx) {
//            File filename = new File(getAlbumStorageDir(context.getApplicationContext()), idx + "");
//            if (filename.exists())
//                filename.delete();
//        }
//    }
//
//    private boolean writePage(int idx, Bitmap b, Point screenSize) {
//        FileOutputStream out = null;
//        File filename = new File(getAlbumStorageDir(getApplicationContext()), idx + "");
//        int width = -1, height = -1;
//        if (screenSize != null) {
//            width = Math.min(screenSize.x, screenSize.y);
//            height = Math.max(screenSize.x, screenSize.y);
//        }
//        try {
//            out = new FileOutputStream(filename);
//            float factor = (float) width / b.getWidth();
//            if (screenSize != null) {
//                Bitmap bb = Bitmap.createScaledBitmap(b, width, (int) (height * factor), false);
//                b.recycle();
//                b = bb;
//            }
//            b.compress(Bitmap.CompressFormat.PNG, 100, out);
//            // PNG is a lossless format, the compression factor (100) is ignored
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (out != null) {
//                    out.close();
//                    return true;
//                }
//                return false;
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//    }

//
//    public static File getAlbumStorageDir(Context context) {
//        if (Utils.isExternalStorageWritable()) {
//            // Get the directory for the app's private pictures directory.
//            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
//                    "quran_Images");
//            if (!file.exists() && !file.mkdirs()) {
//                Log.e("QuranShemerly", "Directory not created");
//            }
//            return file;
//        } else {
//            File file = new File(context.getFilesDir(), "quran_Images");
//            if (!file.exists() && !file.mkdirs()) {
//                Log.e("QuranShemerly", "Directory not created");
//            }
//            return file;
//        }
//    }
//
//    private boolean pageExists(int idx) {
//        File filename = new File(getAlbumStorageDir(getApplicationContext()), idx + "");
//        return filename.exists();
//    }

//
//    private int getNumExistingPages() {
//        int num = 0;
//        for (int i = 1; i <= FullScreenImageAdapter.MAX_PAGE; ++i) {
//            if (pageExists(i))
//                ++num;
//        }
//        return num;
//    }

//private void showNotification() {
//        NotificationCompat.Builder mBuilder =
//        new NotificationCompat.Builder(this)
//        .setSmallIcon(R.mipmap.ic_launcher)
//        .setContentTitle("Thumb File Deleted")
//        .setContentText("A thumb file with size has been deleted");
//        NotificationManager mNotificationManager =
//        (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//// mId allows you to update the notification later on.
//        mNotificationManager.notify(1992, mBuilder.build());
//        }
//
//private void showAlert(String title, String msg) {
//        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
//        dlgAlert.setMessage(msg);
//        dlgAlert.setTitle(title);
//        dlgAlert.setPositiveButton("موافق", null);
//        dlgAlert.setCancelable(false);
//        dlgAlert.create().show();
//        }
//
//private void showConfirm(String title, String msg, DialogInterface.OnClickListener ok) {
//        new AlertDialog.Builder(this)
//        .setIcon(android.R.drawable.ic_dialog_alert)
//        .setTitle(title)
//        .setMessage(msg)
//        .setPositiveButton("نعم", ok)
//        .setNegativeButton("لا", null)
//        .show();
//        }
