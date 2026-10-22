package com.personal.khata;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    LinearLayout root, rows; EditText titleInput, amountInput; Spinner typeSpinner; TextView totalIn, totalOut, balance; JSONArray entries; android.content.SharedPreferences prefs;
    int dp(float n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(20,90,75)); getWindow().setNavigationBarColor(Color.rgb(20,90,75)); prefs=getSharedPreferences("khata",MODE_PRIVATE); buildUI(); loadEntries(); render(); }
    TextView text(String s,int size, boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(30,40,45)); if(bold)t.setTypeface(null,Typeface.BOLD); return t; }
    void buildUI(){
      ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(18),dp(20),dp(18),dp(24)); root.setBackgroundColor(Color.rgb(246,248,247)); scroll.addView(root); setContentView(scroll);
      TextView head=text("My Personal Khata",26,true); head.setTextColor(Color.WHITE); head.setPadding(dp(16),dp(22),dp(16),dp(22)); head.setBackgroundColor(Color.rgb(20,90,75)); root.addView(head);
      TextView sub=text("Income • Expense • Balance",14,false); sub.setPadding(0,dp(12),0,dp(10)); root.addView(sub);
      LinearLayout cards=new LinearLayout(this); cards.setOrientation(1); cards.setPadding(dp(14),dp(14),dp(14),dp(14)); cards.setBackgroundColor(Color.WHITE); root.addView(cards);
      totalIn=text("Total Income: Rs 0",17,true); totalOut=text("Total Expense: Rs 0",17,true); balance=text("Balance: Rs 0",20,true); cards.addView(totalIn); cards.addView(totalOut); cards.addView(balance);
      TextView formTitle=text("Add New Entry",20,true); formTitle.setPadding(0,dp(22),0,dp(8)); root.addView(formTitle);
      typeSpinner=new Spinner(this); String[] types={"Income","Expense"}; typeSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types)); root.addView(typeSpinner);
      titleInput=new EditText(this); titleInput.setHint("Description (e.g. Salary, Grocery)"); titleInput.setSingleLine(true); root.addView(titleInput);
      amountInput=new EditText(this); amountInput.setHint("Amount in Rs"); amountInput.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); amountInput.setSingleLine(true); root.addView(amountInput);
      Button add=new Button(this); add.setText("Save Entry"); add.setTextColor(Color.WHITE); add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(20,90,75))); root.addView(add); add.setOnClickListener(v->saveEntry());
      TextView hist=text("Transaction History",20,true); hist.setPadding(0,dp(20),0,dp(8)); root.addView(hist); rows=new LinearLayout(this); rows.setOrientation(1); root.addView(rows);
      Button backup=new Button(this); backup.setText("Export Backup"); root.addView(backup); backup.setOnClickListener(v->{ String data=prefs.getString("entries","[]"); android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(android.content.Intent.EXTRA_SUBJECT,"My Personal Khata Backup"); i.putExtra(android.content.Intent.EXTRA_TEXT,data); startActivity(android.content.Intent.createChooser(i,"Save or share backup")); });
    }
    void loadEntries(){ try{ entries=new JSONArray(prefs.getString("entries","[]")); }catch(Exception e){entries=new JSONArray();} }
    void saveEntry(){ String name=titleInput.getText().toString().trim(), amt=amountInput.getText().toString().trim(); if(name.isEmpty()){titleInput.setError("Enter description");return;} if(amt.isEmpty()||Double.parseDouble(amt)<=0){amountInput.setError("Enter a valid amount");return;} try{ JSONObject o=new JSONObject(); o.put("description",name); o.put("amount",Double.parseDouble(amt)); o.put("type",typeSpinner.getSelectedItem().toString()); o.put("date",new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.getDefault()).format(new Date())); entries.put(o); prefs.edit().putString("entries",entries.toString()).apply(); titleInput.setText(""); amountInput.setText(""); render(); Toast.makeText(this,"Entry saved",Toast.LENGTH_SHORT).show(); }catch(Exception e){Toast.makeText(this,"Could not save entry",Toast.LENGTH_SHORT).show();} }
    void render(){ if(rows==null)return; rows.removeAllViews(); double in=0,out=0; for(int x=0;x<entries.length();x++){ JSONObject o=entries.optJSONObject(x); if(o==null)continue; double a=o.optDouble("amount"); if("Income".equals(o.optString("type")))in+=a;else out+=a; LinearLayout item=new LinearLayout(this); item.setOrientation(1); item.setPadding(dp(12),dp(10),dp(12),dp(10)); item.setBackgroundColor(Color.WHITE); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(8); rows.addView(item,lp); TextView t=text(o.optString("description")+" — Rs "+String.format(Locale.getDefault(),"%.2f",a),16,true); item.addView(t); TextView d=text(o.optString("type")+" • "+o.optString("date"),12,false); item.addView(d); Button del=new Button(this); del.setText("Delete"); del.setOnClickListener(v->{ JSONArray newer=new JSONArray(); for(int j=0;j<entries.length();j++)if(j!=x)newer.put(entries.opt(j)); entries=newer; prefs.edit().putString("entries",entries.toString()).apply(); render(); }); item.addView(del); } totalIn.setText("Total Income: Rs "+String.format(Locale.getDefault(),"%.2f",in)); totalOut.setText("Total Expense: Rs "+String.format(Locale.getDefault(),"%.2f",out)); balance.setText("Balance: Rs "+String.format(Locale.getDefault(),"%.2f",in-out)); if(entries.length()==0)rows.addView(text("No entries yet.",14,false)); }
}
