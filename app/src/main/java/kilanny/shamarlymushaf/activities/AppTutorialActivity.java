package kilanny.shamarlymushaf.activities;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.PermissionStep;
import kilanny.shamarlymushaf.data.Step;

public class AppTutorialActivity extends TutorialActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addFragment(
                new PermissionStep
                        .Builder()
                        .setPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
                        .setTitle("Need Permission").setContent("Please give us permission")
                        .setBackgroundColor(Color.parseColor("#FF0957"))
                        .setDrawable(android.R.drawable.ic_input_add)
                        .setSummary("Continue and learn")
                        .build());
        addFragment(
                new Step.Builder()
                        .setTitle("Automatic data")
                        .setContent("Friend Photos")
                        .setBackgroundColor(Color.parseColor("#FF0957"))
                        .setDrawable(R.drawable.ic_menu_camera)
                        .setSummary("Continue and learn")
                        .build());
        addFragment(
                new Step.Builder()
                        .setTitle("Choose how to listen")
                        .setContent("Swap the tap to open app and minimize the gap")
                        .setBackgroundColor(Color.parseColor("#00D4BA"))
                        .setDrawable(R.drawable.ic_menu_gallery)
                        .setSummary("Continue and update")
                        .build());
        addFragment(
                new Step.Builder()
                        .setTitle("Edit data")
                        .setContent("You can update data easily")
                        .setBackgroundColor(Color.parseColor("#1098FE"))
                        .setDrawable(R.drawable.ic_menu_send)
                        .setSummary("Continue and result")
                        .build());
        addFragment(
                new Step.Builder()
                        .setTitle("Awesome")
                        .setContent("After updating")
                        .setBackgroundColor(Color.parseColor("#CA70F3"))
                        .setDrawable(R.drawable.ic_menu_slideshow)
                        .setSummary("Continue and thank you")
                        .build());
    }

    @Override
    public void finishTutorial() {
        Toast.makeText(this, "Tutorial finished", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void currentFragmentPosition(int position) {
        Toast.makeText(this, "Position : " + position, Toast.LENGTH_SHORT).show();
    }
}
