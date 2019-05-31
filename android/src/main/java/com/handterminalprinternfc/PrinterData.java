package com.handterminalprinternfc;

import java.util.List;


public class PrinterData {

	Boolean GunSonuMu = new Boolean(false);
	Boolean BaslikYaz = new Boolean(true);
	Boolean ImzaYaz = new Boolean (true);
	
	String FisBaslik = new String();
	String VadeTanim = new String();
	String EksperAdi= new String();
	String AlimYeriAdi = new String();
	String FabrikaAdi = new String();
	Double ToplamAlimKg = new Double(0);
	Double ToplamKesintiKg = new Double(0);
	Double ToplamNetKg= new Double(0);

	List<Alim> Alimlar ;
}


