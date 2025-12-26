from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional
import joblib
import numpy as np

import os

# Load models
current_dir = os.path.dirname(os.path.abspath(__file__))
print(f"DEBUG: Current Dir: {current_dir}")
print(f"DEBUG: CWD: {os.getcwd()}")

try:
    rf_path = os.path.join(current_dir, "cognivox_wesad_rf.joblib")
    print(f"DEBUG: Loading RF from: {rf_path}")
    rf_obj = joblib.load(rf_path)
    rf_model = rf_obj["model"]
    rf_feature_cols = rf_obj["feature_cols"]
except Exception as e:
    print(f"Failed to load RF model: {e}")
    rf_model = None
    rf_feature_cols = []

try:
    lite_path = os.path.join(current_dir, "cognivox_wesad_lite_enhanced.joblib")
    lite_obj = joblib.load(lite_path)
    lite_model = lite_obj["model"]
    lite_feature_cols = lite_obj["feature_cols"]
except Exception as e:
    print(f"Failed to load Lite model: {e}")
    lite_model = None
    lite_feature_cols = []

app = FastAPI()

class FeatureInput(BaseModel):
    # Common or RF specific
    eda_mean: Optional[float] = None
    eda_std: Optional[float] = None
    eda_min: Optional[float] = None
    eda_max: Optional[float] = None
    bvp_mean: float
    bvp_std: float
    temp_mean: Optional[float] = None
    temp_std: Optional[float] = None
    acc_mag_mean: Optional[float] = None
    acc_mag_std: Optional[float] = None
    
    # Lite specific
    bvp_min: Optional[float] = None
    bvp_max: Optional[float] = None
    bvp_range: Optional[float] = None
    bvp_energy: Optional[float] = None
    acc_mean: Optional[float] = None
    acc_std: Optional[float] = None
    acc_max: Optional[float] = None


def predict_stress_from_features_dict(model, feature_cols, feat_dict, threshold=0.6):
    try:
        x = np.array([[feat_dict[col] for col in feature_cols]])
    except KeyError as e:
        raise ValueError(f"Missing feature for selected model: {e}")
        
    proba = model.predict_proba(x)[0, 1]
    label = 1 if proba >= threshold else 0
    return label, float(proba)


def generate_suggestion(label, stress_score):
    if label == 0:
        if stress_score < 0.3:
            return "You are calm. Keep going."
        else:
            return "Slight stress detected. Maintain pace and breathing."
    else:
        if stress_score < 0.75:
            return "You seem stressed. Slow down and take a deep breath."
        else:
            return "High stress detected. Pause, breathe slowly, then continue."

@app.post("/predict_stress")
def predict_stress(input: FeatureInput):
    feat_dict = input.dict()
    
    # Logic: If EDA is present, use RF model. Else use Lite model.
    # We check eda_mean as a proxy for EDA data availability.
    use_rf = (feat_dict.get("eda_mean") is not None)
    
    if use_rf:
        print("DEBUG: EDA data detected. Using RF Model.")
        if rf_model is None:
             raise HTTPException(status_code=500, detail="RF model is not loaded.")
        
        model_name = "RF"
        model = rf_model
        cols = rf_feature_cols
    else:
        print("DEBUG: No EDA data detected. Using Lite Model.")
        if lite_model is None:
             raise HTTPException(status_code=500, detail="Lite model is not loaded.")
        
        model_name = "Lite"
        model = lite_model
        cols = lite_feature_cols

    try:
        label, score = predict_stress_from_features_dict(model, cols, feat_dict)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {e}")

    suggestion = generate_suggestion(label, score)
    return {
        "model_used": model_name,
        "label": int(label),
        "stress_score": score,
        "suggestion": suggestion
    }

if __name__ == "__main__":
    import uvicorn
    print("Starting BioSync Server...")
    print("Listening on 0.0.0.0:8000 (Accessible via local IP)")
    uvicorn.run(app, host="0.0.0.0", port=8000)
