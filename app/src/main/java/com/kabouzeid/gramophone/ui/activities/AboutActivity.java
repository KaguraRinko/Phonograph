package com.kabouzeid.gramophone.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.internal.ThemeSingleton;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.dialogs.ChangelogDialog;
import com.kabouzeid.gramophone.ui.activities.base.AbsBaseActivity;
import com.kabouzeid.gramophone.ui.activities.bugreport.BugReportActivity;
import com.kabouzeid.gramophone.ui.activities.intro.AppIntroActivity;

import de.psdev.licensesdialog.LicensesDialog;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@SuppressWarnings("FieldCanBeLocal")
public class AboutActivity extends AbsBaseActivity implements View.OnClickListener {

    private static String GITHUB = "https://github.com/kabouzeid/Phonograph";

    private static String TWITTER = "https://twitter.com/swiftkarim";
    private static String WEBSITE = "https://kabouzeid.com/";

    private static String TRANSLATE = "https://phonograph.oneskyapp.com/collaboration/project?id=26521";
    private static String RATE_ON_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=com.kabouzeid.gramophone";

    private static String AIDAN_FOLLESTAD_GITHUB = "https://github.com/afollestad";

    private static String MICHAEL_COOK_WEBSITE = "https://cookicons.co/";

    private static String MAARTEN_CORPEL_WEBSITE = "https://maartencorpel.com/";
    private static String MAARTEN_CORPEL_TWITTER = "https://twitter.com/maartencorpel";

    private static String ALEKSANDAR_TESIC_TWITTER = "https://twitter.com/djsalezmaj";

    private static String EUGENE_CHEUNG_GITHUB = "https://github.com/arkon";
    private static String EUGENE_CHEUNG_WEBSITE = "https://echeung.me/";

    private static String ADRIAN_TWITTER = "https://twitter.com/froschgames";

    Toolbar toolbar;
    TextView appVersion;
    LinearLayout changelog;
    LinearLayout intro;
    LinearLayout licenses;
    LinearLayout writeAnEmail;
    LinearLayout followOnTwitter;
    LinearLayout forkOnGitHub;
    LinearLayout visitWebsite;
    LinearLayout reportBugs;
    LinearLayout translate;
    LinearLayout rateOnGooglePlay;
    AppCompatButton aidanFollestadGitHub;
    AppCompatButton michaelCookWebsite;
    AppCompatButton maartenCorpelWebsite;
    AppCompatButton maartenCorpelTwitter;
    AppCompatButton aleksandarTesicTwitter;
    AppCompatButton eugeneCheungGitHub;
    AppCompatButton eugeneCheungWebsite;
    AppCompatButton adrianTwitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setDrawUnderStatusbar();
        toolbar = findViewById(R.id.toolbar);
        appVersion = findViewById(R.id.app_version);
        changelog = findViewById(R.id.changelog);
        intro = findViewById(R.id.intro);
        licenses = findViewById(R.id.licenses);
        writeAnEmail = findViewById(R.id.write_an_email);
        followOnTwitter = findViewById(R.id.follow_on_twitter);
        forkOnGitHub = findViewById(R.id.fork_on_github);
        visitWebsite = findViewById(R.id.visit_website);
        reportBugs = findViewById(R.id.report_bugs);
        translate = findViewById(R.id.translate);
        rateOnGooglePlay = findViewById(R.id.rate_on_google_play);
        aidanFollestadGitHub = findViewById(R.id.aidan_follestad_git_hub);
        michaelCookWebsite = findViewById(R.id.michael_cook_website);
        maartenCorpelWebsite = findViewById(R.id.maarten_corpel_website);
        maartenCorpelTwitter = findViewById(R.id.maarten_corpel_twitter);
        aleksandarTesicTwitter = findViewById(R.id.aleksandar_tesic_twitter);
        eugeneCheungGitHub = findViewById(R.id.eugene_cheung_git_hub);
        eugeneCheungWebsite = findViewById(R.id.eugene_cheung_website);
        adrianTwitter = findViewById(R.id.adrian_twitter);

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        setUpViews();
    }

    private void setUpViews() {
        setUpToolbar();
        setUpAppVersion();
        setUpOnClickListeners();
    }

    private void setUpToolbar() {
        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setUpAppVersion() {
        appVersion.setText(getCurrentVersionName(this));
    }

    private void setUpOnClickListeners() {
        changelog.setOnClickListener(this);
        intro.setOnClickListener(this);
        licenses.setOnClickListener(this);
        followOnTwitter.setOnClickListener(this);
        forkOnGitHub.setOnClickListener(this);
        visitWebsite.setOnClickListener(this);
        reportBugs.setOnClickListener(this);
        writeAnEmail.setOnClickListener(this);
        translate.setOnClickListener(this);
        rateOnGooglePlay.setOnClickListener(this);
        aidanFollestadGitHub.setOnClickListener(this);
        michaelCookWebsite.setOnClickListener(this);
        maartenCorpelWebsite.setOnClickListener(this);
        maartenCorpelTwitter.setOnClickListener(this);
        aleksandarTesicTwitter.setOnClickListener(this);
        eugeneCheungGitHub.setOnClickListener(this);
        eugeneCheungWebsite.setOnClickListener(this);
        adrianTwitter.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String getCurrentVersionName(@NonNull final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "Unkown";
    }

    @Override
    public void onClick(View v) {
        if (v == changelog) {
            ChangelogDialog.create().show(getSupportFragmentManager(), "CHANGELOG_DIALOG");
        } else if (v == licenses) {
            showLicenseDialog();
        } else if (v == intro) {
            startActivity(new Intent(this, AppIntroActivity.class));
        } else if (v == followOnTwitter) {
            openUrl(TWITTER);
        } else if (v == forkOnGitHub) {
            openUrl(GITHUB);
        } else if (v == visitWebsite) {
            openUrl(WEBSITE);
        } else if (v == reportBugs) {
            startActivity(new Intent(this, BugReportActivity.class));
        } else if (v == writeAnEmail) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:contact@kabouzeid.com"));
            intent.putExtra(Intent.EXTRA_EMAIL, "contact@kabouzeid.com");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Phonograph");
            startActivity(Intent.createChooser(intent, "E-Mail"));
        } else if (v == translate) {
            openUrl(TRANSLATE);
        } else if (v == rateOnGooglePlay) {
            openUrl(RATE_ON_GOOGLE_PLAY);
        } else if (v == aidanFollestadGitHub) {
            openUrl(AIDAN_FOLLESTAD_GITHUB);
        } else if (v == michaelCookWebsite) {
            openUrl(MICHAEL_COOK_WEBSITE);
        } else if (v == maartenCorpelWebsite) {
            openUrl(MAARTEN_CORPEL_WEBSITE);
        } else if (v == maartenCorpelTwitter) {
            openUrl(MAARTEN_CORPEL_TWITTER);
        } else if (v == aleksandarTesicTwitter) {
            openUrl(ALEKSANDAR_TESIC_TWITTER);
        } else if (v == eugeneCheungGitHub) {
            openUrl(EUGENE_CHEUNG_GITHUB);
        } else if (v == eugeneCheungWebsite) {
            openUrl(EUGENE_CHEUNG_WEBSITE);
        } else if (v == adrianTwitter) {
            openUrl(ADRIAN_TWITTER);
        }
    }

    private void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void showLicenseDialog() {
        new LicensesDialog.Builder(this)
                .setNotices(R.raw.notices)
                .setTitle(R.string.licenses)
                .setNoticesCssStyle(getString(R.string.license_dialog_style)
                        .replace("{bg-color}", ThemeSingleton.get().darkTheme ? "424242" : "ffffff")
                        .replace("{text-color}", ThemeSingleton.get().darkTheme ? "ffffff" : "000000")
                        .replace("{license-bg-color}", ThemeSingleton.get().darkTheme ? "535353" : "eeeeee")
                )
                .setIncludeOwnLicense(true)
                .build()
                .show();
    }
}
