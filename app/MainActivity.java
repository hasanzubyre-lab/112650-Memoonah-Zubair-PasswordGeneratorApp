package com.example.passwordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.security.SecureRandom;

public class MainActivity extends AppCompatActivity {

    private TextView tvPassword, tvLengthValue;
    private EditText etPasswordLength;
    private SeekBar seekBarLength;
    private CheckBox cbUppercase, cbLowercase, cbNumbers, cbSpecialChars;
    private Button btnGenerate, btnCopy, btnRefresh;

    // Character sets
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private SecureRandom random = new SecureRandom();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initViews();

        // Load saved preferences
        prefs = getSharedPreferences("PasswordPrefs", MODE_PRIVATE);
        loadPreferences();

        // Setup listeners
        setupListeners();

        // Generate initial password
        generatePassword();
    }

    private void initViews() {
        tvPassword = findViewById(R.id.tvPassword);
        tvLengthValue = findViewById(R.id.tvLengthValue);
        etPasswordLength = findViewById(R.id.etPasswordLength);
        seekBarLength = findViewById(R.id.seekBarLength);
        cbUppercase = findViewById(R.id.cbUppercase);
        cbLowercase = findViewById(R.id.cbLowercase);
        cbNumbers = findViewById(R.id.cbNumbers);
        cbSpecialChars = findViewById(R.id.cbSpecialChars);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnCopy = findViewById(R.id.btnCopy);
        btnRefresh = findViewById(R.id.btnRefresh);
    }

    private void setupListeners() {
        // SeekBar listener
        seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 4) {
                    progress = 4;
                    seekBar.setProgress(progress);
                }
                etPasswordLength.setText(String.valueOf(progress));
                tvLengthValue.setText("Length: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // EditText listener
        etPasswordLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int length = Integer.parseInt(s.toString());
                    if (length < 4) {
                        length = 4;
                        etPasswordLength.setText("4");
                    } else if (length > 32) {
                        length = 32;
                        etPasswordLength.setText("32");
                    }
                    seekBarLength.setProgress(length);
                    tvLengthValue.setText("Length: " + length);
                } catch (NumberFormatException e) {
                    seekBarLength.setProgress(8);
                    etPasswordLength.setText("8");
                    tvLengthValue.setText("Length: 8");
                }
            }
        });

        // Checkbox listeners to save preferences
        CompoundButton.OnCheckedChangeListener checkListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                savePreferences();
                if (!isAnyCheckboxChecked()) {
                    cbLowercase.setChecked(true);
                    Toast.makeText(MainActivity.this, "At least one option must be selected!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        cbUppercase.setOnCheckedChangeListener(checkListener);
        cbLowercase.setOnCheckedChangeListener(checkListener);
        cbNumbers.setOnCheckedChangeListener(checkListener);
        cbSpecialChars.setOnCheckedChangeListener(checkListener);

        // Button listeners
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePassword();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePassword();
            }
        });

        // Long press to copy
        tvPassword.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyToClipboard();
                return true;
            }
        });
    }

    private boolean isAnyCheckboxChecked() {
        return cbUppercase.isChecked() || cbLowercase.isChecked() ||
                cbNumbers.isChecked() || cbSpecialChars.isChecked();
    }

    private void generatePassword() {
        if (!isAnyCheckboxChecked()) {
            cbLowercase.setChecked(true);
            Toast.makeText(this, "Lowercase selected by default", Toast.LENGTH_SHORT).show();
        }

        int length;
        try {
            length = Integer.parseInt(etPasswordLength.getText().toString());
        } catch (NumberFormatException e) {
            length = 8;
            etPasswordLength.setText("8");
        }

        // Build character pool
        StringBuilder charPool = new StringBuilder();
        if (cbUppercase.isChecked()) charPool.append(UPPERCASE);
        if (cbLowercase.isChecked()) charPool.append(LOWERCASE);
        if (cbNumbers.isChecked()) charPool.append(NUMBERS);
        if (cbSpecialChars.isChecked()) charPool.append(SPECIAL_CHARS);

        // Generate password
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charPool.length());
            password.append(charPool.charAt(randomIndex));
        }

        // Ensure at least one character from each selected category
        if (length >= 4) {
            password = ensureOneFromEach(password, length);
        }

        tvPassword.setText(password.toString());
    }

    private StringBuilder ensureOneFromEach(StringBuilder password, int length) {
        StringBuilder result = new StringBuilder(password);

        // Check each selected category
        if (cbUppercase.isChecked() && !containsAny(result.toString(), UPPERCASE)) {
            result.setCharAt(random.nextInt(length), UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        }
        if (cbLowercase.isChecked() && !containsAny(result.toString(), LOWERCASE)) {
            result.setCharAt(random.nextInt(length), LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        }
        if (cbNumbers.isChecked() && !containsAny(result.toString(), NUMBERS)) {
            result.setCharAt(random.nextInt(length), NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        }
        if (cbSpecialChars.isChecked() && !containsAny(result.toString(), SPECIAL_CHARS)) {
            result.setCharAt(random.nextInt(length), SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        }

        return result;
    }

    private boolean containsAny(String str, String chars) {
        for (char c : chars.toCharArray()) {
            if (str.indexOf(c) >= 0) return true;
        }
        return false;
    }

    private void copyToClipboard() {
        String password = tvPassword.getText().toString();
        if (password.isEmpty() || password.equals("Click Generate")) {
            Toast.makeText(this, "No password to copy!", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Password", password);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Password copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("uppercase", cbUppercase.isChecked());
        editor.putBoolean("lowercase", cbLowercase.isChecked());
        editor.putBoolean("numbers", cbNumbers.isChecked());
        editor.putBoolean("special", cbSpecialChars.isChecked());
        editor.putInt("length", seekBarLength.getProgress());
        editor.apply();
    }

    private void loadPreferences() {
        cbUppercase.setChecked(prefs.getBoolean("uppercase", true));
        cbLowercase.setChecked(prefs.getBoolean("lowercase", true));
        cbNumbers.setChecked(prefs.getBoolean("numbers", true));
        cbSpecialChars.setChecked(prefs.getBoolean("special", false));

        int savedLength = prefs.getInt("length", 12);
        seekBarLength.setProgress(savedLength);
        etPasswordLength.setText(String.valueOf(savedLength));
        tvLengthValue.setText("Length: " + savedLength);
    }
}
