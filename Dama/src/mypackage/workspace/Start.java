package mypackage.workspace;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

//PAGINA INIZIALE DOVE INSERISCO I NOMI DEI GIOCATORI E NELLA PAGINA DOPO DECIDO CHI GIOCA


public class Start extends Activity {
	  private EditText nomeProprio, nomeAvversario,passMia;
	    private Button gioca;
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.start);
	        nomeProprio = (EditText) findViewById(R.id.nomeProprio);
	        nomeAvversario = (EditText) findViewById(R.id.nomeAvversario);
	        passMia= (EditText) findViewById(R.id.passMia);
	        gioca = (Button) findViewById(R.id.gioca);
	        
	        gioca.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent startIntent = new Intent(Start.this, Main.class);
					String me = nomeProprio.getText().toString();
					String you =nomeAvversario.getText().toString();
					String pass=passMia.getText().toString();
					
					startIntent.putExtra("NOMEPROP",me);
					startIntent.putExtra("NOMEAVV",you);
					startIntent.putExtra("PASSMIA",pass);
					//tmp = nomeProprio.getText().toString() + " Vs " +nomeAvversario.getText().toString();
					//startIntent.putExtra("PLAYERS",tmp);

					startActivity(startIntent);
					
				}
			});
	        
	        
	    }
	    
	    
	    
	    /*public void play(View v){
	    	Intent startIntent = new Intent(this, Main.class);
			String tmp = nomeProprio.getText().toString();
			startIntent.putExtra("NOMEPROP",tmp);
			tmp = nomeAvversario.getText().toString();
			startIntent.putExtra("NOMEAVV",tmp);
			startActivity(startIntent);
	    	
	    }*/
	}