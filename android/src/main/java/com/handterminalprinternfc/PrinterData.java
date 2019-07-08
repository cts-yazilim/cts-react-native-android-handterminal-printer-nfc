package com.handterminalprinternfc;

import java.util.List;


public class PrinterData {

	String FisBaslik = new String();
	String VadeTanim = new String();
	String EksperAdi= new String();
	String AlimYeriAdi = new String();
	String FabrikaAdi = new String();
	Double ToplamAlimKg = new Double(0);
	Double ToplamKesintiKg = new Double(0);
	Double ToplamNetKg= new Double(0);

	Alim Alim  = new Alim(); 						// Normal Fis cikartma isleminde tek alim olarak bu degisken kullanilmaktadir.
	List<VadeAlim> Vadeler ; // Gunsonu Aliminda liste olarak vadeler gruplanmis halde bu degiskene atanmaktadir.
}


