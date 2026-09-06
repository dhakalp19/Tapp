package com.prakash.tasknotes;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    final ArrayList<Item> tasks = new ArrayList<>(), notes = new ArrayList<>();
    SharedPreferences prefs; LinearLayout list; TextView status; Button taskTab, noteTab, add; boolean taskMode = true;
    int purple = Color.rgb(79,70,229), bg = Color.rgb(246,247,251), muted = Color.rgb(105,110,122);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); prefs=getSharedPreferences("tasknotes",MODE_PRIVATE); load(); setContentView(screen()); refresh();
    }

    View screen(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(14)); root.setBackgroundColor(bg);
        TextView title=text("Task Notes",30,Color.rgb(25,28,35),true); root.addView(title);
        status=text("",14,muted,false); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.bottomMargin=dp(14); root.addView(status,sp);
        LinearLayout tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        taskTab=button("Tasks"); noteTab=button("Notes"); tabs.addView(taskTab,new LinearLayout.LayoutParams(0,dp(48),1)); tabs.addView(noteTab,new LinearLayout.LayoutParams(0,dp(48),1)); root.addView(tabs);
        taskTab.setOnClickListener(v->{taskMode=true;refresh();}); noteTab.setOnClickListener(v->{taskMode=false;refresh();});
        ScrollView sc=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(0,dp(12),0,dp(10)); sc.addView(list); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        add=button("+ Add task"); add.setTextColor(Color.WHITE); add.setBackground(round(purple,16)); add.setOnClickListener(v->{if(taskMode)addTask();else addNote();}); root.addView(add,new LinearLayout.LayoutParams(-1,dp(54)));
        return root;
    }

    void refresh(){
        int open=0, imp=0; for(Item i:tasks)if(!i.flag)open++; for(Item i:notes)if(i.flag)imp++;
        status.setText(open+" open task"+(open==1?"":"s")+"  •  "+imp+" important note"+(imp==1?"":"s"));
        taskTab.setTextColor(taskMode?purple:muted); noteTab.setTextColor(taskMode?muted:purple); add.setText(taskMode?"+ Add task":"+ Add note"); list.removeAllViews();
        if(taskMode) showTasks(); else showNotes();
    }

    void showTasks(){
        if(tasks.isEmpty()){empty("No tasks yet","Tap Add task to create one.");return;}
        for(Item i:new ArrayList<>(tasks)){
            LinearLayout row=card(); row.setGravity(Gravity.CENTER_VERTICAL); CheckBox cb=new CheckBox(this); cb.setChecked(i.flag); row.addView(cb);
            LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); TextView t=text(i.title,17,i.flag?muted:Color.rgb(25,28,35),true); if(i.flag)t.setPaintFlags(t.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG); copy.addView(t); if(!i.body.isEmpty())copy.addView(text(i.body,14,muted,false)); row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            Button del=small("Delete"); row.addView(del,new LinearLayout.LayoutParams(dp(82),dp(42))); cb.setOnCheckedChangeListener((x,c)->{i.flag=c;save();refresh();}); del.setOnClickListener(v->confirm("Delete task?",i.title,()->{tasks.remove(i);save();refresh();})); addCard(row);
        }
    }

    void showNotes(){
        if(notes.isEmpty()){empty("No notes yet","Tap Add note to save something useful.");return;}
        ArrayList<Item> sorted=new ArrayList<>(notes); sorted.sort((a,b)->Boolean.compare(b.flag,a.flag));
        for(Item i:sorted){
            LinearLayout box=card(); box.setOrientation(LinearLayout.VERTICAL); LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); head.addView(text(i.title,18,Color.rgb(25,28,35),true),new LinearLayout.LayoutParams(0,-2,1)); Button star=small(i.flag?"★":"☆"); head.addView(star,new LinearLayout.LayoutParams(dp(54),dp(42))); box.addView(head); TextView body=text(i.body,15,Color.rgb(60,65,75),false); body.setPadding(0,dp(7),0,dp(10)); box.addView(body);
            LinearLayout actions=new LinearLayout(this); Button share=small("Share"), del=small("Delete"); actions.addView(share,new LinearLayout.LayoutParams(0,dp(42),1)); actions.addView(del,new LinearLayout.LayoutParams(0,dp(42),1)); box.addView(actions);
            star.setOnClickListener(v->{i.flag=!i.flag;save();refresh();}); share.setOnClickListener(v->share(i)); del.setOnClickListener(v->confirm("Delete note?",i.title,()->{notes.remove(i);save();refresh();})); addCard(box);
        }
    }

    void addTask(){
        LinearLayout f=form(); EditText title=field("Task title",false), details=field("Details (optional)",true); f.addView(title); f.addView(details,new LinearLayout.LayoutParams(-1,dp(100)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Add task").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Add",null).create(); d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String s=title.getText().toString().trim(); if(s.isEmpty()){title.setError("Enter a task");return;} tasks.add(0,new Item(s,details.getText().toString().trim(),false));save();d.dismiss();refresh();})); d.show();
    }

    void addNote(){
        LinearLayout f=form(); EditText title=field("Note title",false), body=field("Write your note",true); f.addView(title); f.addView(body,new LinearLayout.LayoutParams(-1,dp(150)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Add note").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create(); d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String a=title.getText().toString().trim(), z=body.getText().toString().trim(); if(a.isEmpty()){title.setError("Enter a title");return;} if(z.isEmpty()){body.setError("Write something");return;} notes.add(0,new Item(a,z,false));save();d.dismiss();refresh();})); d.show();
    }

    void share(Item i){ Intent s=new Intent(Intent.ACTION_SEND); s.setType("text/plain"); s.putExtra(Intent.EXTRA_SUBJECT,i.title); s.putExtra(Intent.EXTRA_TEXT,i.title+"\n\n"+i.body); startActivity(Intent.createChooser(s,"Share note")); }
    void confirm(String title,String msg,Runnable yes){ new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->yes.run()).show(); }

    LinearLayout card(){ LinearLayout v=new LinearLayout(this); v.setPadding(dp(14),dp(12),dp(14),dp(12)); v.setBackground(round(Color.WHITE,16)); v.setElevation(dp(1)); return v; }
    void addCard(View v){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(10); list.addView(v,p); }
    void empty(String a,String b){ LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setGravity(Gravity.CENTER); x.setPadding(0,dp(70),0,0); TextView t=text(a,20,Color.DKGRAY,true); t.setGravity(Gravity.CENTER); TextView z=text(b,15,muted,false); z.setGravity(Gravity.CENTER); x.addView(t); x.addView(z); list.addView(x); }
    LinearLayout form(){ LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(dp(22),dp(8),dp(22),0); return x; }
    EditText field(String hint,boolean multi){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setPadding(dp(10),dp(10),dp(10),dp(10)); if(multi){e.setGravity(Gravity.TOP);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);} return e; }
    Button button(String s){ Button b=new Button(this); b.setAllCaps(false); b.setText(s); b.setTextSize(16); return b; }
    Button small(String s){ Button b=button(s); b.setTextSize(14); b.setMinWidth(0); b.setMinimumWidth(0); return b; }
    TextView text(String s,int sz,int c,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sz); t.setTextColor(c); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    GradientDrawable round(int c,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(c); g.setCornerRadius(dp(r)); return g; }
    int dp(int x){ return Math.round(x*getResources().getDisplayMetrics().density); }

    void save(){ try{ JSONArray a=new JSONArray(), n=new JSONArray(); for(Item i:tasks)a.put(i.json()); for(Item i:notes)n.put(i.json()); prefs.edit().putString("tasks",a.toString()).putString("notes",n.toString()).apply(); }catch(Exception ignored){} }
    void load(){ try{ JSONArray a=new JSONArray(prefs.getString("tasks","[]")), n=new JSONArray(prefs.getString("notes","[]")); for(int x=0;x<a.length();x++)tasks.add(Item.from(a.getJSONObject(x))); for(int x=0;x<n.length();x++)notes.add(Item.from(n.getJSONObject(x))); }catch(Exception ignored){} }
    static class Item{ String title,body; boolean flag; Item(String t,String b,boolean f){title=t;body=b;flag=f;} JSONObject json()throws Exception{JSONObject o=new JSONObject();o.put("title",title);o.put("body",body);o.put("flag",flag);return o;} static Item from(JSONObject o){return new Item(o.optString("title"),o.optString("body"),o.optBoolean("flag"));} }
}
