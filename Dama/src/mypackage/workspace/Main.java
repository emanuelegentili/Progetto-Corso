package mypackage.workspace;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Main extends Activity implements MessageReceiver {
	// disable screen rotation in android applicatione (stackoverflow)
	protected static final int SHOW_TOAST = 0;
	protected static final int FAI_MOSSA = 1;
	protected static final int HAI_PERSO = 2;

	TextView tv;
	ConnectionManager connection;
	private Stato statoCorrente;
	private EditText nomeProprio, nomeAvversario;
	public String TAG = "workspace.Dama1.main";
	int vite_mie = 12, vite_avv = 12;

	// variabili che mi servono per la dama,ossia l'array e le immagini e il
	// colore delle mie pedine
	static int[] array_gioco;
	Resources res;
	static boolean colore = false, miocolore = false;

	private String pack = "", pack_arr = "";

	int pedina_selezionata, riga_selezionata, colonna_selezionata;

	enum Stato {
		WAIT_FOR_START, WAIT_FOR_START_ACK, TOCCA_LUI, TOCCA_ME, DEVO_MUOVERE, HO_PERSO, HO_VINTO
	}

	/*
	 * tocca a lui sto in attesa tocca a me: seleziono ciò che voglio muovere e
	 * inserisce i vincoli della pedina devo_muovere:dopo aver deciso la pedina
	 * verifico se ci posso andare,se si verifico se mangio e se ho vinto
	 */

	// decido chi comincia tramite nome avversari qui inserisco timer

	Timer timer = new Timer();
	TimerTask sendStart = new TimerTask() {

		@Override
		public void run() { // TODO Auto-generated method stub
			if (statoCorrente == Stato.WAIT_FOR_START_ACK) {
				connection.send("START");
			} else {
				Log.d(TAG, "Sending START but the state is " + statoCorrente);
			}
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// PRENDO I NOMI
		String nomeProprio = getIntent().getExtras().getString("NOMEPROP");
		String password = getIntent().getExtras().getString("PASS");
		String nomeAvversario = getIntent().getExtras().getString("NOMEAVV");

		// String players = getIntent().getExtras().getString("PLAYERS");
		// tv.setText(nomeProprio + " Vs " + nomeAvversario);
		// INIZIALIZZAZIONE CONNESSIONE
		connection = new ConnectionManager(nomeProprio, password,
				nomeAvversario, this);

		// verifico chi inizia prima e verifico il mio COLORE
		if (nomeAvversario.hashCode() < nomeProprio.hashCode()) {
			// Inizio io
			statoCorrente = Stato.WAIT_FOR_START_ACK;
			timer.schedule(sendStart, 1000, 5000);
		//	statoCorrente = Stato.WAIT_FOR_START_ACK;
			colore = true; // sono bianco
		} else {
			// Inizia lui
			// Io aspetto il pacchetto;
			statoCorrente = Stato.WAIT_FOR_START;
			colore = false; // sono il nero

		}

		res = getResources();
		array_gioco = new int[64];

		costruisco_scacchiera();

		inizializzo_squadre();

		GridView gridView = (GridView) findViewById(R.id.miaGriglia);
		stampa();

		// gridView.setPadding(left, top, right, bottom)
		// gridView.generateLayoutParams(attrs)

		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				int row = (position / 8) + 1;
				int column = (position % 8) + 1;
				/*
				 * Toast toast = Toast.makeText(Main.this, "Hai toccato riga " +
				 * row + " colonna " + column, Toast.LENGTH_SHORT);
				 * toast.show();
				 */

				gestione_click(row, column);

			}
		});

	}

	public void gestione_click(int row, int column) {

		if (statoCorrente == Stato.TOCCA_ME) {

			if (ver_casella_colore(pos_array(row, column)) == false) {
				Toast toast = Toast.makeText(Main.this, "casella errata",
						Toast.LENGTH_SHORT);
				toast.show();
			} else { // se entra significa che ho premuto la casella con una mia
						// pedina
				riga_selezionata = row;
				colonna_selezionata = column;
				pedina_selezionata = pos_array(row, column);
				int a = array_gioco[pos_array(riga_selezionata,
						colonna_selezionata)];
				array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = contenuto_casella_selezionata(
						row, column); // evidenzio la casella
				stampa();
				array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = a;
				// faccio la prima parte del pacchetto indico se dama o pedina e
				// le posizioni
				impacchetto(pedina_o_dama_string(riga_selezionata,colonna_selezionata), riga_selezionata,
						colonna_selezionata);

				// gli restituisco il suo valore
				// qua ristampo la scacchiera con quella selezionata e le
				// posizioni dove posso andare
				statoCorrente = Stato.DEVO_MUOVERE;
			}
		} else if (statoCorrente == Stato.DEVO_MUOVERE) {
			if (ver_casella_movimento(row, column) == false) {
				Toast toast = Toast.makeText(Main.this,
						"Non può andare in quella casella", Toast.LENGTH_SHORT);
				toast.show();
				svuoto_pacchetto();
				stampa();
				statoCorrente = Stato.TOCCA_ME;
			} else { // significa che ci posso andare
				if (array_gioco[pos_array(row, column)] == 0) {
					// muovo il pezzo senza problemi
					array_gioco[pos_array(row, column)] = array_gioco[pos_array(
							riga_selezionata, colonna_selezionata)];
					array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = 0;
					converto_in_dama(row, column);

					impacchetto("M", row, column);
					statoCorrente = Stato.TOCCA_LUI;
					chiudo_pacchetto();
					connection.send("VAI:" + pack);
					svuoto_pacchetto();
					stampa();

				} else {
					// se non è libera significa che posso mangiare
					if (mangiata_singola(row, column) != 0) { // posso mangiare
																// e ci vado
						mangio(row, column);
						chiudo_pacchetto();
						// mangiata_doppia
						// verifica trasforma dama e se se qualcuno ha vinto
						// converto_in_dama(row, column);
						stampa();
						if (vite_avv == 0) {
							statoCorrente = Stato.HO_VINTO;
							connection.send("HAI_PERSO!" + pack);
							svuoto_pacchetto();
							Toast toast = Toast.makeText(Main.this,
									"Hai vinto", Toast.LENGTH_SHORT);
							toast.show();

						} else {
							connection.send("VAI:" + pack);
							statoCorrente = Stato.TOCCA_LUI;
							svuoto_pacchetto();
						}
						// devo inserire mangiata doppia
					} else { // altirmenti mi ritrovo nell stesso stato di prima
						Toast toast = Toast
								.makeText(
										Main.this,
										"Non posso mangiare! Seleziona un altra casella",
										Toast.LENGTH_SHORT);
						toast.show();
						svuoto_pacchetto();
						stampa();
						statoCorrente = Stato.TOCCA_ME;
					}
				}
			}
		} else if (statoCorrente == Stato.TOCCA_LUI) {
			Toast toast = Toast.makeText(Main.this,
					"ASPETTA! tocca al tuo avversario!", Toast.LENGTH_SHORT);
			toast.show();

		} else if (statoCorrente == Stato.HO_PERSO) {
			Toast toast = Toast.makeText(Main.this, "Hai perso!!",
					Toast.LENGTH_SHORT);
			toast.show();

		} else if (statoCorrente == Stato.HO_VINTO) {
			Toast toast = Toast.makeText(Main.this, "Hai vinto!!",
					Toast.LENGTH_SHORT);
			toast.show();

		}

	}

	@Override
	public void receiveMessage(String msg) {
		// inserisco i tipi di messaggi ricevuti e faccio i vari controlli con i
		// stati
		if (msg.equals("START")) {
			if (statoCorrente == Stato.WAIT_FOR_START){
			connection.send("COMINCIA");
			statoCorrente=Stato.TOCCA_LUI;
			}
			else{
				Log.e(TAG, "Ricevuto START ma lo stato è " + statoCorrente);
			}
		}
		else if(msg.equals("COMINCIA")){
			
			Message osmsg = handler.obtainMessage(Main.SHOW_TOAST);
			Bundle b = new Bundle();
			b.putString("toast", "Vai!!"); // invio il boundle con la stringa e
											// il pacchetto mosse
			osmsg.setData(b);
			handler.sendMessage(osmsg);
			
			timer.cancel();
			statoCorrente = Stato.TOCCA_ME;
				
		
			
		} else if (msg.startsWith("VAI")) {
			pack_arr = msg.split(":")[1]; // splitta in array diviso da :

			Message osmsg = handler.obtainMessage(Main.FAI_MOSSA);
			Bundle b = new Bundle();
			b.putString("mossa", pack_arr); // invio il boundle con la stringa e
											// il pacchetto mosse
			osmsg.setData(b);
			handler.sendMessage(osmsg);

			statoCorrente = Stato.TOCCA_ME;
			svuoto_pacchettoARR(); // svuoto il pacchetto,non dovrebbe servire

		} else if (msg.startsWith("HAI_PERSO")) {
			pack_arr = msg.split("!")[1];

			Message osmsg = handler.obtainMessage(Main.HAI_PERSO);
			Bundle b = new Bundle();
			b.putString("mossa", pack_arr); // invio il boundle con la stringa e
											// il pacchetto mosse
			osmsg.setData(b);
			handler.sendMessage(osmsg);
			svuoto_pacchettoARR();
			statoCorrente = Stato.HO_PERSO;
		}

	}

	// connessione
	final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Main.FAI_MOSSA:
				String mossa = msg.getData().getString("mossa");
				Log.d(TAG, "dentro handler è arrivata la mossa " + mossa);
				decompilatore(mossa);
				stampa();
				break;

			case Main.HAI_PERSO:
				Toast toast = Toast.makeText(Main.this, "Hai perso!!",
						Toast.LENGTH_SHORT);
				toast.show();
				String mossaVinc = msg.getData().getString("mossa");
				decompilatore(mossaVinc);
				stampa();
				break;
				
			case Main.SHOW_TOAST:
				String message = msg.getData().getString("toast");
				Toast toast2 = Toast.makeText(Main.this, message,
						Toast.LENGTH_SHORT);
				toast2.show();
				Log.d(TAG, "dentro handler è arrivata la mossa " + message);
				break;

			default:
				super.handleMessage(msg);
			}
		}
	};

	// per trasferimento messaggio
	void impacchetto(String lettera, int row, int column) {
		pack += lettera + (9 - row) + (9 - column);
	}

	void chiudo_pacchetto() {
		pack += "F";
	}

	void svuoto_pacchetto() {
		pack = "";
	}

	void svuoto_pacchettoARR() {
		pack_arr = "";
	}

	void decompilatore(String pack_arr) {
		/*
		 * pack_arr è una string dove sono elencate le mosse che ha fatto
		 * l'avversario ogni 3 caratteri definisco una dama con mossa: il primo
		 * elemento definisco cosa ha mosso (tipo: P24 (pedina riga2colonna4) o
		 * D per dama) dopo in poi E per eat(mangiato) con riga e colonne oppure
		 * M per move nella riga colonna il pezzo P o D definito prima,qui
		 * faccio anche controllo se si è mosso in riga 8 allora converto pedona
		 * in dama e dopo di tutto F per definire la fine
		 */
		int i = 0, riga = 0, colonna = 0;
		String tipo = "";
		int riga_tipo = 0, colonna_tipo = 0;
		Log.d(TAG, "pack_arr= " + pack_arr);

		while (!(pack_arr.substring(i, i + 1)).equals("F")) { // fintanto che
																// la lettera è
																// diversa da F

			if ((pack_arr.substring(i, i + 1)).equals("P")
					|| (pack_arr.substring(i, i + 1)).equals("D")) { // ho
																		// spostato
																		// la
																		// pedina
																		// dalla
																		// posizione
																		// precedente
				tipo = pack_arr.substring(i, i + 1);
				i++;
				riga_tipo = Integer.parseInt(pack_arr.substring(i, i + 1));
				i++;
				colonna_tipo = Integer.parseInt(pack_arr.substring(i, i + 1));
				array_gioco[pos_array(riga_tipo, colonna_tipo)] = 0;
			} else if ((pack_arr.substring(i, i + 1)).equals("E")) { // ho
																		// mangiato
																		// la
																		// pedina
																		// in
																		// quella
																		// posizione
				i++;
				riga = Integer.parseInt(pack_arr.substring(i, i + 1));
				i++;
				colonna = Integer.parseInt(pack_arr.substring(i, i + 1));
				array_gioco[pos_array(riga, colonna)] = 0;
			} else if ((pack_arr.substring(i, i + 1)).equals("M")) { // ha
																		// mosso
																		// la
																		// pedina
																		// in(sia
																		// se ha
																		// mangiato
																		// che
																		// no)
				i++;
				riga = Integer.parseInt(pack_arr.substring(i, i + 1));
				i++;
				colonna = Integer.parseInt(pack_arr.substring(i, i + 1));

				if (tipo.equals("P")) { // se è una pedina
					if (converto_in_damaB((9 - riga), colonna_tipo) == false) {
						array_gioco[pos_array(riga, colonna)] = restit_pedina(!colore); // !colore
																						// per
																						// restituire
																						// la
																						// pedina
																						// dell'avversario
					} else {
						array_gioco[pos_array(riga, colonna)] = restit_dama(!colore);
					}
				} else { // altirmenti è una dama
					array_gioco[pos_array(riga, colonna)] = restit_dama(!colore);
				}
			}
			i++; // vado al passo successivo
		}
	}

	void pack_mangio(int row, int column) {

		pack = (pack + "E" + (9 - row) + (9 - column));

	}

	int pos_array(int row, int column) {
		int count = 8;
		count = ((count * row) - (8 - column)) - 1;

		return count;
	}

	boolean pos_arrayB(int row, int column) {
		if (row > 8 || column > 8) {
			return false;
		} else {
			return true;
		}
	}

	void stampa() {
		// CONVERTO array di gioco con array per la stampa
		ImageButton[] imgb = new ImageButton[64];
		Drawable[] img = new Drawable[64];

		for (int i = 0; i < 64; i++) {

			if (array_gioco[i] == 0) {
				img[i] = res.getDrawable(R.drawable.nero1);
				// imgb[i].setImageDrawable(res.getDrawable(R.drawable.nero));
			} else if (array_gioco[i] == 1) {
				img[i] = res.getDrawable(R.drawable.bianco1);
				// imgb[i].setImageDrawable(res.getDrawable(R.drawable.bianco));
			} else if (array_gioco[i] == 2) {
				img[i] = res.getDrawable(R.drawable.pedinabianca1);
				// imgb[i].setImageDrawable(res.getDrawable(R.drawable.pedinabianca));
			} else if (array_gioco[i] == 3) {
				img[i] = res.getDrawable(R.drawable.pedinanera1);
				// imgb[i].setImageDrawable(res.getDrawable(R.drawable.pedinanera));
			} else if (array_gioco[i] == 4) {
				img[i] = res.getDrawable(R.drawable.damabianca1);
				// imgb[i].setImageDrawable(res.getDrawable(R.drawable.damabianca));
			} else if (array_gioco[i] == 5) {
				img[i] = res.getDrawable(R.drawable.damanera1);
				// imgb[i].setImageDrawable(res.getDrawable(R.drawable.damanera));
			} else if (array_gioco[i] == 6) {
				img[i] = res.getDrawable(R.drawable.pedinabiancaselezionata1);
			} else if (array_gioco[i] == 7) {
				img[i] = res.getDrawable(R.drawable.pedinaneraselezionata1);
			} else if (array_gioco[i] == 8) {
				img[i] = res.getDrawable(R.drawable.damabiancaselezionata1);
			} else if (array_gioco[i] == 9) {
				img[i] = res.getDrawable(R.drawable.damaneraselezionata1);
			}

		}

		GridView gridView = (GridView) findViewById(R.id.miaGriglia);
		gridView.setAdapter(new ImageAdapter(this, img));
	}

	void converto_in_dama(int row, int column) {

		if (row == 1) {
			if (!colore) {
				array_gioco[pos_array(row, column)] = 5;
			} else {
				array_gioco[pos_array(row, column)] = 4;
			}

		}
	}

	boolean converto_in_damaB(int row, int column) {

		if (row == 1) {
			return true;
		} else
			return false;
	}

	// funzioni di costruzione iniziale
	void costruisco_scacchiera() {
		int i, j;
		boolean cambio = false;
		// COSTRUISCO LA SCACCHIERA
		for (i = 0; i < 64; i++) {
			if (!cambio) {
				array_gioco[i] = 0;
				cambio = true;
			} else {
				array_gioco[i] = 1;
				cambio = false;
			}

			if (i % 8 == 7) {
				if (!cambio) {
					cambio = true;
				} else {
					cambio = false;
				}
			}
		}
	}

	void inizializzo_squadre() {
		int i;
		boolean cambio = false;
		// metto le squadre a inizio partita dopo aver verificato il mio colore
		for (i = 0; i < 23; i++) {
			// metto la prima squadra
			if (!cambio) {
				array_gioco[i] = restit_pedina(!colore);
				cambio = true;
			} else {
				cambio = false;
			}

			if (i % 8 == 7) {
				if (!cambio) {
					cambio = true;
				} else {
					cambio = false;
				}
			}
		}

		for (i = 40; i < 64; i++) {
			// metto la seconda squadra
			if (!cambio) {
				array_gioco[i] = restit_pedina(colore);
				cambio = true;
			} else {
				cambio = false;
			}

			if (i % 8 == 7) {
				if (!cambio) {
					cambio = true;
				} else {
					cambio = false;
				}
			}
		}
	}

	// funzioni di verifica movimento

	boolean ver_casella_colore(int posizione_array) { // dice se ho selezionato
														// una pedina del mio
														// colore

		if (colore == false) { // se io sono nero
			if (array_gioco[posizione_array] == 3 // deve essere uan pedina nera
					| array_gioco[posizione_array] == 5) { // deve essere una
															// dama nera

				return true;
			}
		} else if (array_gioco[posizione_array] == 2 // altrimenti significa che
														// sono bianco
				| array_gioco[posizione_array] == 4) {

			return true;
		}

		return false; // non ha passato nessun controllo

	}

	boolean ver_casella_movimento(int row, int column) {
		// faccio i due casi se pedina o dama
		if (array_gioco[pos_array(riga_selezionata, colonna_selezionata)] == 2
				|| array_gioco[pos_array(riga_selezionata, colonna_selezionata)] == 3) { // se
																							// è
																							// una
																							// pedina
			if (row == (riga_selezionata - 1)
					&& column == (colonna_selezionata - 1)
					|| row == (riga_selezionata - 1)
					&& column == (colonna_selezionata + 1)) {
				return true;
			} else
				return false;

		} else if (array_gioco[pos_array(riga_selezionata, colonna_selezionata)] == 4
				|| array_gioco[pos_array(riga_selezionata, colonna_selezionata)] == 5) { // se
																							// è
																							// una
																							// dama
			if (row == (riga_selezionata - 1)
					&& column == (colonna_selezionata - 1)
					|| row == (riga_selezionata + 1)
					&& column == (colonna_selezionata + 1)
					|| row == (riga_selezionata - 1)
					&& column == (colonna_selezionata + 1)
					|| row == (riga_selezionata + 1)
					&& column == (colonna_selezionata - 1)) {
				return true;
			} else
				return false;
		}
		return false;
	}

	// mai utilizzate
	void muovo_easy(int row, int column) { // MAI UTILIZZATA, ma già
											// implementata
											// IN GESTIONE CLICK IN DEVO MUOVERE

		if (!colore) { // MI MUOVO SE POSSO se sono nero
			if (array_gioco[pos_array(riga_selezionata, colonna_selezionata)] == 3) { // se
																						// è
																						// una
																						// pedina
				array_gioco[pos_array(row, column)] = 3;
			} else {
				array_gioco[pos_array(row, column)] = 5; // se è una dama
			}
		} else { // se sono bianco
			if (array_gioco[pos_array(riga_selezionata, colonna_selezionata)] == 2) {
				array_gioco[pos_array(row, column)] = 2;
			} else {
				array_gioco[pos_array(row, column)] = 4;
			}

		}
		array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = 0; // svuoto
																			// la
																			// casella
																			// della
																			// posizione
																			// precedente

	}

	// funzioni utili per interrogare
	int restit_pedina(boolean colore) {
		if (!colore) {
			return 3;
		} else
			return 2;
	}

	int restit_dama(boolean colore) {
		if (!colore) {
			return 5;
		} else
			return 4;
	}

	int pedina_o_dama(int row, int column) { // mi dice se in quella casella c'è
												// una pedina o una dama
		if (array_gioco[pos_array(row, column)] == 2
				|| array_gioco[pos_array(row, column)] == 3) {
			return 1;
		}
		if (array_gioco[pos_array(row, column)] == 4
				|| array_gioco[pos_array(row, column)] == 5) {
			return 2;
		} else
			return 0; // non dovrebbe darlo mai
	}

	String pedina_o_dama_string(int row, int column) { // mi dice se in quella
														// casella c'è
		// una pedina o una dama
		if (array_gioco[pos_array(row, column)] == 2
				|| array_gioco[pos_array(row, column)] == 3) {
			return "P";
		}
		if (array_gioco[pos_array(row, column)] == 4
				|| array_gioco[pos_array(row, column)] == 5) {
			return "D";
		} else
			return " "; // non dovrebbe darlo mai
	}

	int contenuto_casella(int row, int column) { // mi dice se in quella casella
													// c'è una pedina o una dama
		if (array_gioco[pos_array(row, column)] == 2)
			return 2;
		if (array_gioco[pos_array(row, column)] == 3)
			return 3;
		if (array_gioco[pos_array(row, column)] == 4)
			return 4;
		if (array_gioco[pos_array(row, column)] == 5)
			return 5;
		return 0;
	}

	int contenuto_casella_selezionata(int row, int column) {
		if (array_gioco[pos_array(row, column)] == 2)
			return 6;
		if (array_gioco[pos_array(row, column)] == 3)
			return 7;
		if (array_gioco[pos_array(row, column)] == 4)
			return 8;
		if (array_gioco[pos_array(row, column)] == 5)
			return 9;
		return 0;
	}

	// per mangiare
	void mangio(int row, int column) {
		int a = contenuto_casella(riga_selezionata, colonna_selezionata);

		// mai uscire tramite questo
		if (pos_arrayB(row, column)) { // controllo inutile, sono sempre
										// ammissibili in questo caso row column
			if (mangiata_singola(row, column) == 0) {
				Toast toast = Toast.makeText(Main.this, "Non posso mangiare",
						Toast.LENGTH_SHORT);
				toast.show();
			} else if (mangiata_singola(row, column) == 1) { // avanti a destra
				array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = 0;
				array_gioco[pos_array(row, column)] = 0;
				pack_mangio(row, column);
				array_gioco[pos_array(row - 1, column + 1)] = a;
				impacchetto("M", row - 1, column + 1);
				converto_in_dama(row - 1, column + 1);

				vite_avv -= 1;

			} else if (mangiata_singola(row, column) == 2) { // avanti a
																// sinistra
				array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = 0;
				array_gioco[pos_array(row, column)] = 0;
				pack_mangio(row, column);
				array_gioco[pos_array(row - 1, column - 1)] = a;
				impacchetto("M", row - 1, column - 1);
				converto_in_dama(row - 1, column - 1);
				vite_avv -= 1;

			} else if (mangiata_singola(row, column) == 3) { // indietro a
																// destra se
																// è una dama
				array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = 0;
				array_gioco[pos_array(row, column)] = 0;
				pack_mangio(row, column);
				array_gioco[pos_array(row + 1, column + 1)] = a;
				impacchetto("M", row + 1, column + 1);
				converto_in_dama(row + 1, column + 1);
				vite_avv -= 1;

			} else if (mangiata_singola(row, column) == 4) { // indietro a
																// sinistra
																// se è una dama
				array_gioco[pos_array(riga_selezionata, colonna_selezionata)] = 0;
				array_gioco[pos_array(row, column)] = 0;
				pack_mangio(row, column);
				array_gioco[pos_array(row + 1, column - 1)] = a;
				impacchetto("M", row + 1, column - 1);
				converto_in_dama(row + 1, column - 1);
				vite_avv -= 1;
			}
		}

	}

	boolean cosa_mangio(int row, int column) { // se pedina mangia pedina o dama
												// mangia dama e funzionare
												// magicamente anche per il
												// colore :o
		if (!colore) {
			if (array_gioco[pos_array(row, column)] == 2
					|| (array_gioco[pos_array(riga_selezionata,
							colonna_selezionata)] == 5 && array_gioco[pos_array(
							row, column)] == 4)) {
				return true;
			} else
				return false;
		} else {
			if (array_gioco[pos_array(row, column)] == 3
					|| (array_gioco[pos_array(riga_selezionata,
							colonna_selezionata)] == 4 && array_gioco[pos_array(
							row, column)] == 5)) {
				return true;
			} else
				return false;
		}
	}

	int mangiata_singola(int row, int column) {// mi dice solamente che se
												// mangio vado in posizione
												// ammissibile??

		if (cosa_mangio(row, column)) { // se pedina mangia pedina o dama
										// mangia dama

			if (riga_selezionata > row && colonna_selezionata < column) { // avanti
																			// a
																			// destra
				if (array_gioco[pos_array(row - 1, column + 1)] == 0
						&& pos_arrayB(row - 1, column + 1)) {
					return 1;
				} else
					return 0;
			} else if (riga_selezionata > row && colonna_selezionata > column) { // avanti
																					// a
																					// sinistra
				if (array_gioco[pos_array(row - 1, column - 1)] == 0
						&& pos_arrayB(row - 1, column - 1)) {
					return 2;
				} else
					return 0;
			} else if (riga_selezionata < row
					&& colonna_selezionata < column
					&& ((array_gioco[pos_array(riga_selezionata,
							colonna_selezionata)] == 5) || (array_gioco[pos_array(
							riga_selezionata, colonna_selezionata)] == 4))) {
				// indietro a
				// destra se è
				// una dama
				if (array_gioco[pos_array(row + 1, column + 1)] == 0
						&& pos_arrayB(row + 1, column + 1)) {
					return 3;
				} else
					return 0;

			} else if (riga_selezionata < row
					&& colonna_selezionata > column
					&& ((array_gioco[pos_array(riga_selezionata,
							colonna_selezionata)] == 5) || (array_gioco[pos_array(
							riga_selezionata, colonna_selezionata)] == 4))) {
				// indietro a
				// sinistra se è
				// una dama
				if (array_gioco[pos_array(row + 1, column - 1)] == 0
						&& pos_arrayB(row + 1, column - 1)) {
					return 4;
				} else
					return 0;
			} else
				return 0; // non dovrebbe mai restituire false da qua,
							// solo per levare errore

		} else {
			return 0;
		}

	}

}