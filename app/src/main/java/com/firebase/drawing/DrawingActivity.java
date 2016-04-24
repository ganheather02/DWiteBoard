package com.firebase.drawing;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DrawingActivity extends ActionBarActivity implements ColorPickerDialog.OnColorChangedListener {
    public static final int THUMBNAIL_SIZE = 256;

    private static final int COLOR_MENU_ID = Menu.FIRST;
    private static final int CLEAR_MENU_ID = COLOR_MENU_ID + 1;
    private static final int PIN_MENU_ID = CLEAR_MENU_ID + 1;
    private static final int UPLOAD_PICTURE_ID = PIN_MENU_ID + 1;
    public static final String TAG = "AndroidDrawing";

    private DrawingView mDrawingView;
    private Firebase mFirebaseRef; // Firebase base URL
    private Firebase mMetadataRef;
    private Firebase mSegmentsRef;
    private Firebase mPictureRef;

    private ValueEventListener mConnectedListener;

    private String mBoardId;
    private int mBoardWidth;
    private int mBoardHeight;

    public String fetchedPicture;
    public String encodedBitmap;
    public Bitmap fetchedBitmap;
    public static Bitmap resizedBitmap;


    public static boolean shouldChangeBackground = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        final String url = intent.getStringExtra("FIREBASE_URL");
        final String boardId = intent.getStringExtra("BOARD_ID");
        Log.i(TAG, "Adding DrawingView on "+url+" for boardId "+boardId);

        mFirebaseRef = new Firebase(url);
        mBoardId = boardId;
        mMetadataRef = mFirebaseRef.child("boardmetas").child(boardId);
        mSegmentsRef = mFirebaseRef.child("boardsegments").child(mBoardId);
        mPictureRef = mFirebaseRef.child("boardmetas").child(mBoardId).child("pictureid");
        mMetadataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mDrawingView != null) {
                    ((ViewGroup) mDrawingView.getParent()).removeView(mDrawingView);
                    mDrawingView.cleanup();
                    mDrawingView = null;
                }
                Map<String, Object> boardValues = (Map<String, Object>) dataSnapshot.getValue();
                if (boardValues != null && boardValues.get("width") != null && boardValues.get("height") != null) {
                    mBoardWidth = ((Long) boardValues.get("width")).intValue();
                    mBoardHeight = ((Long) boardValues.get("height")).intValue();

                    mDrawingView = new DrawingView(DrawingActivity.this, mFirebaseRef.child("boardsegments").child(boardId), mBoardWidth, mBoardHeight);
                    setContentView(mDrawingView);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set up a notification to let us know when we're connected or disconnected from the Firebase servers
        mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(DrawingActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DrawingActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });

        mPictureRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchedPicture = dataSnapshot.getValue(String.class);
                if(fetchedPicture != null) {
                    try {
                        fetchedBitmap = decodeFromBase64(fetchedPicture);

                        if(fetchedBitmap != null){Toast.makeText(DrawingActivity.this, "Picture Decoded", Toast.LENGTH_SHORT).show();}
                        DisplayMetrics metrics = new DisplayMetrics();

                        getWindowManager().getDefaultDisplay().getMetrics(metrics);

                        int height = metrics.heightPixels;
                        int width = metrics.widthPixels;

                        resizedBitmap = myResizedBitmap(fetchedBitmap, width, height);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    shouldChangeBackground = true;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Toast.makeText(DrawingActivity.this, "Data Cancel (Picture)", Toast.LENGTH_SHORT).show();
            }
        });

        /*Firebase picturesRef = mRootRef.child("pictureid");
        picturesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchedPicture = dataSnapshot.getValue(String.class);
                Toast.makeText(DrawingActivity.this, "Fetched Picture", Toast.LENGTH_SHORT).show();
                //Log.v("E_VALUE", fetchedPicture);
                shouldChangeBackground = true;
                try{
                    fetchedBitmap = decodeFromBase64(fetchedPicture);
                    //DisplayMetrics metrics = new DisplayMetrics();

                    //getWindowManager().getDefaultDisplay().getMetrics(metrics);

                    //int height = metrics.heightPixels;
                    //int width = metrics.widthPixels;

                    //resizedBitmap = getResizedBitmap(fetchedBitmap, width, height);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        }); */
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up our listener so we don't have it attached twice.
        mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        if (mDrawingView != null) {
            mDrawingView.cleanup();
        }
        this.updateThumbnail(mBoardWidth, mBoardHeight, mSegmentsRef, mMetadataRef);

        shouldChangeBackground = false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // getMenuInflater().inflate(R.menu.menu_drawing, menu);

        menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c').setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, CLEAR_MENU_ID, 2, "Clear").setShortcut('5', 'x');
        menu.add(0, PIN_MENU_ID, 3, "Keep in sync").setShortcut('6', 's').setIcon(android.R.drawable.ic_lock_lock)
                .setCheckable(true).setChecked(SyncedBoardManager.isSynced(mBoardId));
        menu.add(0, UPLOAD_PICTURE_ID, 3, "Upload Picture").setShortcut('6', 's').setIcon(android.R.drawable.ic_lock_lock);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == COLOR_MENU_ID) {
            new ColorPickerDialog(this, this, 0xFFFF0000).show();
            return true;
        } else if (item.getItemId() == CLEAR_MENU_ID) {
            mDrawingView.cleanup();
            mSegmentsRef.removeValue(new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    if (firebaseError != null) {
                        throw firebaseError.toException();
                    }
                    mDrawingView = new DrawingView(DrawingActivity.this, mFirebaseRef.child("boardsegments").child(mBoardId), mBoardWidth, mBoardHeight);
                    setContentView(mDrawingView);
                    //mDrawingView.clear();
                }
            });

            return true;
        } else if (item.getItemId() == PIN_MENU_ID) {
            SyncedBoardManager.toggle(mFirebaseRef.child("boardsegments"), mBoardId);
            item.setChecked(SyncedBoardManager.isSynced(mBoardId));
            return true;
        } else if (item.getItemId() == UPLOAD_PICTURE_ID) {
            Intent i = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            final int ACTIVITY_SELECT_IMAGES = 1234;
            startActivityForResult(i, ACTIVITY_SELECT_IMAGES);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case 1234:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    Bitmap yourselectedImage = BitmapFactory.decodeFile(filePath);

                    Bitmap convertedImage = getResizedBitmap(yourselectedImage, 500);

                    encodedBitmap = encodeToBase64(convertedImage);

                    //mMetadataRef.push().setValue(encodedBitmap);

                    mMetadataRef.child("pictureid").setValue(encodedBitmap, new Firebase.CompletionListener(){
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                Log.e(TAG, "Error updating thumbnail", firebaseError.toException());
                            }
                        }
                    });
                }
        }
    }

    public static void updateThumbnail(int boardWidth, int boardHeight, Firebase segmentsRef, final Firebase metadataRef) {
        final float scale = Math.min(1.0f * THUMBNAIL_SIZE / boardWidth, 1.0f * THUMBNAIL_SIZE / boardHeight);
        final Bitmap b = Bitmap.createBitmap(Math.round(boardWidth * scale), Math.round(boardHeight * scale), Bitmap.Config.ARGB_8888);
        final Canvas buffer = new Canvas(b);

        buffer.drawRect(0, 0, b.getWidth(), b.getHeight(), DrawingView.paintFromColor(Color.WHITE, Paint.Style.FILL_AND_STROKE));
        Log.i(TAG, "Generating thumbnail of " + b.getWidth() + "x" + b.getHeight());

        segmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot segmentSnapshot : dataSnapshot.getChildren()) {
                    Segment segment = segmentSnapshot.getValue(Segment.class);
                    buffer.drawPath(
                            DrawingView.getPathForPoints(segment.getPoints(), scale),
                            DrawingView.paintFromColor(segment.getColor())
                    );
                }
                String encoded = encodeToBase64(b);
                metadataRef.child("thumbnail").setValue(encoded, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if (firebaseError != null) {
                            Log.e(TAG, "Error updating thumbnail", firebaseError.toException());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public static String encodeToBase64(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = com.firebase.client.utilities.Base64.encodeBytes(b);

        return imageEncoded;
    }
    public static Bitmap decodeFromBase64(String input) throws IOException {
        byte[] decodedByte = com.firebase.client.utilities.Base64.decode(input);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    @Override
    public void colorChanged(int newColor) {
        mDrawingView.setColor(newColor);
    }

    public Bitmap myResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }
}
