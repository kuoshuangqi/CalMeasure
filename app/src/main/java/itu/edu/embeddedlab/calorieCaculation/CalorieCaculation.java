package itu.edu.embeddedlab.calorieCaculation;

import itu.edu.embeddedlab.calmeasure.Constant;

public class CalorieCaculation {
	private int subPosture; //should have three different postures !!!!!!!!!!!!!!!!!!!!
	private double tsk; //tsk,i (C)
	private double tsk0; //tsk,i - 1 (C)
	private double ta; //C
	private double tr; //Mean radiant temperature (C)
	private double pa; //Partial water vapour pressure kilopascals++++++++++++++++
	private double va; // Air velocity meters per second
	private double walksp; // Walking speed meters per seconds
	private double icl; // Static thermal insulation clo
	private double ap;  //Fraction of the body surface covered (#should have three different fraction by covering)!!!!!!!!!!!!!!!!!!!
	private double fr;  //Reflection coefficients, Fr, for different special materials, cotton = 0.42 (#should have different number by materials)!!!!!!!!!!!!!!!!!!!!!
	//Emissivity of the reflective clothing dimensionless (by default: Fr=0.97)
	private double theta; // Angle between walking direction and wind direction degrees
	private boolean defdir; // walking direction entered
	private boolean defspeed; // walking speed entered
	private double swpMeasured; // sweat rate, t = i gh-1
	private double swp0Measured;  // sweat rate, t = i - 1 gh-1
	private double weight; // kg
	private double height; // m

	//Constant
	private double imst;  //Static moisture permeability index. Mean value imist equal to 0.38

	// Transformation
	private double swp ; //sweat rate, t = i watts per square meter
	private double swp0; // sweat rate, t = i - 1 watts per square meter

	// Initialisation
	private double tre;
	private double tcreq;
	private double tcreq0;
	private double tsktcrwg;
	private double tsktcrwg0;
	private double met; //Metabolic rate : Watts per square meter (hypothesized value)

	private double psk; // Water vapour pressure at skin temperature
	private double adu; //DuBois body surface area(BSA is in m2, W is mass in kg, and H is height in cm.): ******
	private double spheat;
	private double constteq;
	private double cconsttsk;
	private double constsw;
	private double last;
	private double swmax;
	private double metDet;
	//temprate 

	public CalorieCaculation(Builder builder){
		initConstant();
		updateAll(builder);
	}
	
	public void initConstant(){
		imst = 0.38;
		
		//EXPONENTIAL AVERAGING CONSTANTS
		constteq = Math.pow(Math.E, -1d / 10d); //// Core temperature as a function of the metabolic rate: time constant: 10 minutes
		cconsttsk = Math.pow(Math.E, -1d / 3d); //onSkin Temperature: time constant: 3 minutes
		constsw = Math.pow(Math.E, -1d / 10d);  //Sweat rate: time constant: 10 minutes
		
		last = 0.111;
	}
	
	public void updateAll(Builder builder){
		subPosture = builder.subPosture; 
		tsk = builder.tsk; 
		tsk0 = builder.tsk0; 
		ta = builder.ta; 
		tr = builder.tr; 
		pa = builder.pa; 
		va = builder.va;
		walksp = builder.walksp; 
		icl = builder.icl; 
		ap = builder.ap;  
		fr = builder.fr;  
		theta = builder.theta; 
		defdir = builder.defdir; 
		defspeed = builder.defspeed; 
		swpMeasured = builder.swpMeasured; 
		swp0Measured = builder.swp0Measured; 
		weight = builder.weight; 
		height = builder.height; 
		tre = builder.tre;
		swp = builder.swp;
		tcreq = builder.tcreq;
		tcreq0 = tcreq;
		tsktcrwg = builder.tsktcrwg;
		met = builder.met; 
		swp = swpMeasured / 2.6;
		swp0 = swp0Measured / 2.6;
		swp = 0;
		tsktcrwg0 = tsktcrwg;
		psk = 0.6105 * Math.exp(17.27 * (tsk - 283) / tsk);
		adu = 0.007184 * Math.pow(weight, 0.425) * Math.pow(height, 0.725);
		spheat = 57.83 * weight / adu;
		double ardu = updateArdu(); //The fraction of skin surface involved in heat exchange by radiation Ar/Adu : ardu
		double iclst = icl * 0.155; //# Static clothing insulation
		double fcl = 1 + 0.3 * icl; //Clothing area factor
		double itotst = iclst + last / fcl;
		double var = updateVar();
		double vaux = updateVaux(var);
//		System.out.println("vaux" + vaux);
		
		double waux = updateWaux();
		double corcl = updateCorcl(vaux, waux);
		double coria = updateCoria(var, waux);
		double cortot = updateCortot(coria, corcl);
		double itotdyn = itotst * cortot;
		double iadyn = coria * last;
		double icldyn = itotdyn - iadyn / fcl;
		//Correction for wind and walking
		double core = (2.6 * cortot - 6.5) * cortot + 4.9;
		double imdyn = updateImdyn(core);
		//Dynamic evaporative resistance
		double rtdyn = itotdyn / (imdyn / 16.7);
		// Dynamic convection coefficient
		// Dynamic convective heat transfer coefficient
		double hcdyn = Math.max(Math.max((2.38 * Math.pow(Math.abs(tsk - ta), 0.025)), (3.5 + 5.2 * var)), (8.7 * Math.pow(var, 0.6)));
		double fclr = (1 - ap)* 0.97 + ap * fr;
		// Dynamic convection coefficient : auxr
		double auxr = Math.pow(5.67, -8d) * ardu;
		//Mean temperature of the clothing: tcl
		double tcl = tr + 0.1;
		double hr = fclr * auxr * (Math.pow((tcl + 273) , 4) - Math.pow((tr + 273) , 4)) / (tcl - tr);
		double tcl1 = ((fcl * (hcdyn * ta + hr * tr) + tsk / icldyn) / (fcl * (hcdyn + hr) + 1 / icldyn));
		tcl = Math.abs(tr + 0.1 - tcl1) > 0.001 ? (tr + 0.1 + tcl1) / 2 : tr + 0.1; 
		//Convection and Radiation heat exchanges
		double conv = fcl * hcdyn * (tcl - ta);
		double rad = fcl * hr * (tcl - tr);
		// Heat flow by convection at the skin surface
		double c = hcdyn * fcl * (tcl - ta);

		// Heat flow by radiation at the surface of the skin
		double r = hr * fcl * (tcl - tr);


		//Calculation of Ereq:
		double emax = (psk - pa)/rtdyn; // Maximum Evaporation Rate
		double swreq = (swp - 0.9048 * swp0) / 0.952;
		double ereq = updateEreq(swreq, emax);
		// Constant for calculation:
		double const_a = 0.0152;	
		double const_a1 = 28.56 + 0.885 * ta + 0.64 * pa;
		double const_b = 0.00127;
		double const_b1 = 59.34 + 0.53 * ta - 0.63 * pa;
		double const_c = 0.0036;
		double const_d = 1 - constteq;
//		System.out.println("tcreq0 * constteq " + tcreq0 + " " + constteq);
		double const_e = tcreq0 * constteq;
		double const_k = 1 - tsktcrwg0;
		double const_j = 1 - const_a * const_a1 - const_b * const_b1;
//		System.out.println("spheat const_k const_e const_c const_d tcreq0 "+ spheat + " " + const_k + " " + const_e + " " + const_c + " " + const_d + " " + tcreq0);
		double const_q = spheat * const_k * (const_e - 55 * const_c * const_d + 36.8 * const_d - tcreq0);
		double const_p = spheat * const_k * const_c * const_d;
		
//		System.out.println("ereq " + ereq);
//		System.out.println("c r ereq const_q constP" + c + " " + r + " " + ereq + " " + const_q + " " + const_p);
		// Calculation of normal metabolic rate:
		double metNormal = (c + r + ereq + const_q) / (r - const_p);
//		System.out.println("metNormal " + metNormal);
		double metCritical = updateMetCritical(emax, const_j, const_p, const_q, c, r, adu);
		
		if(metNormal < metCritical){
			metDet = metNormal;
		}else{
			metDet = updateMetDet(metCritical, ereq, emax, swp0);
		}

		metDet = metDet / 4.186 * adu;
	}
	
	public double getCaloire(){
		return metDet;
	}
	
	private double updateMetDet(double metCritical, double ereq, double emax, double swp0){
		double metM = metCritical * 1.2;
		double swp_predict = swDet(metM, ereq, emax, swp0);
		double tsk_predict = tskDet(metM);
		
		while(! (Math.abs(swp - swp_predict) / swmax < 0.01 && Math.abs(tsk-tsk_predict) < 0.1)){
			if(swp < swp_predict && tsk < tsk_predict){
				metM = metM * 1.05;
			}else{
				metM = metM * 0.95;
			}
			swp_predict = swDet(metM, ereq, emax, swp0);
			tsk_predict = tskDet(metM);
		}
		return metM;
//	    	    while not (abs(swp - swp_predict) / swmax < 0.01 and abs(tsk - tsk_predict) < 0.1):
//	    	        if swp < swp_predict and tsk < tsk_predict:
//	    	            met_m = met_m * 1.05
//	    	        else:
//	    	            met_m = met_m * 0.95
//	    	        swp_predict = sw_det(met_m, ereq, swreq, emax, constsw, swp0)
//	    	        tsk_predict = tsk_det(met_m, ta, tr, pa, va, consttsk, tsk0)
//	    	    met_det = met_m
	}
	
	private double tskDet(double metM){
		tre = 36.8;
	    // skin temperature in equilibrium: clothed model
	    double tskeqcl = 12.165 + 0.0217 * ta + 0.04361 * tr + 0.19354 * pa - 0.25315 * va;
	    tskeqcl = tskeqcl + 0.005346 * metM + 0.51274;
	    
	    //skin temperature in equilibrium: nude model
	    double tskeqnu = 7.191 + 0.64 * ta + 0.061 * tr + 0.198 * pa - 0.348 * va;
	    tskeqnu = tskeqnu + 0.616 * tre;
	    
	    double tskeq = 0d;
	    if(icl >= 0.6){
	    	tskeq = tskeqcl;
	    }else if(icl <= 0.2){
	    	tskeq = tskeqnu;
	    }else{
	    	tskeq = tskeqnu + 2.5 * (tskeqcl - tskeqnu) * (icl - 0.2);
	    }
	    return tsk0 * cconsttsk + tskeq * (1 - cconsttsk);
	}
	
	private double swDet(double metM, double ereq, double emax, double swp0){
	    double wreq = ereq / emax;
	    double _swreq = 0d;
	    double _swmax = (metM - 32) * adu;
	    if(_swmax > 400){
	    	swmax = 400;
	    }else if(_swmax < 250){
	    	swmax = 250;
	    }else{
	    	 swmax = _swmax;
	    }

	    if(ereq <= 0){ // no sweat rate
	    	_swreq = 0;
	    }else if(emax <= 0){ // sweat rate is maximum
	    	emax = 0;
	    	_swreq = swmax;
	    }else if(wreq >= 1.7){ // sweat rate maximum
	    	 wreq = 1.7;
	    	 _swreq = swmax;
	    }else{
	    	double eveff = 0d;
	    	if(wreq > 1){
	    		eveff = Math.pow((2 - wreq) , 2) / 2;
	    	}else{
	    		eveff = (1- Math.pow(wreq, 2)) / 2;
	    	}

	    	_swreq = ereq / eveff;

	    	if(_swreq > swmax){
	    		_swreq = swmax;
	    	}else{
	    	    _swreq = _swreq;
	    	}
	    }	            
	    // Predicted sweat rate
	    return swp0 * constsw + _swreq * (1 - constsw);
	}
	
	private double updateMetCritical(double emax , double const_j, double const_p, double const_q, double c, double r, double adu){
	    double m1 = (c + r + 1.7 * emax) / (const_j - const_p);
	    double m2 = (c + r - 32 * 0.045 * adu + const_q) / (const_j - const_p - 0.045 * adu);
	    return m1 > m2 ? m2 : m1;
	}
	
	private double updateEreq(double swreq, double emax){
		double result = 1 / (2 * swreq) * ((-2 * Math.pow(emax , 2)) + 
				Math.pow(((2 * Math.pow(emax, 2)) - (4 * swreq * (-1 * Math.pow(emax , 2) * swreq))), 0.5));
		return result / emax <= 1 ? result : 1 / (2 * swreq) * ((4 * swreq * emax + 2 * Math.pow(emax , 2)) + 
				Math.pow((Math.pow((4 * swreq * Math.pow(emax, 2)) , 2) - 16 * Math.pow(swreq , 2) * Math.pow(emax , 2 )) , 0.5));
	}
	
	private double updateImdyn(double core){
		double imdyn = imst * core;
		return imdyn > 0.9 ? 0.9 : imdyn;
	}
	
	private double updateCortot(double coria, double corcl){
		return icl <= 0.6 ? ((0.6 - icl) * coria + icl * corcl) : corcl;
	}
	
	private double updateCoria(double var, double waux){
		double result = Math.exp((0.047 * var - 0.472) * var + (0.117 * waux - 0.342) * waux);
		return result > 1? 1 : result;
	}
	
	private double updateCorcl(double vaux, double waux){
		double result = 1.044 * Math.exp((0.066 * vaux - 0.398) * vaux + (0.094 * waux - 0.378) * waux);
		return result > 1? 1 : result;
	}
	
	private double updateWaux(){
		double result = 0.0d;
		if(walksp > 1.5){
			result = 1.5;
		}else if(walksp <= 0.7){
			result = 0.0052 * (met - 58);
		}else{
			result = walksp;
		}
		return result;
	}
	
	// Clothing insulation correction for wind (Var) and walking (Walksp)
	private double updateVaux(double var){
		return var > 3? 3d : var;
	}
	
	private double updateArdu(){
		double result = 0.0;
		switch(subPosture){
			case Constant.SUBPOSTURE_CROUCHING:
				result = 0.67;
				break;
			case Constant.SUBPOSTURE_SEATED:
				result = 0.7;
				break;
			case Constant.SUBPOSTURE_STANDING:
				result = 0.77;
				break;
				default:
					break;
		}
		return result;
	}
	
	//Relative velocities due to air velocity and movements
	private double updateVar(){
		double result = 0d;
		if(defspeed){
			if(defdir){ //Unidirectional walking
				double x = Math.cos(3.14159 * theta / 180);
				result = Math.abs(va - walksp * x);
			}else{ //Omni-directional walking
				if(va < walksp){
					result = walksp;
				}else{
					result = va;
				}
			}
		}else{ //Stationary or undefined speed
			result = va;
		}
		return result;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Builder builder = new Builder();
		CalorieCaculation calorieCaculation = builder.setSubPosture(Constant.SUBPOSTURE_STANDING).setTsk(34.1).setTsk0(34.1)
				.setTa(40).setTr(40).setPa(2.5).setVa(0.3).setWalksp(0).setIcl(0.5).setAp(0.54).setFr(0.42).setTheta(0).setDefdir(true)
				.setDefspeed(true).setSwpMeasured(700).setSwp0Measured(699).setWeight(75).setHeight(1.8).setTre(36.8).setSwp(0).setTcreq(36.8)
				.setTsktcrwg(0.3).setMet(150).build();
		System.out.println(" the calorie is " + calorieCaculation.getCaloire());
		builder.setWeight(74).setHeight(1.75).updateAll();
		System.out.println(" the calorie is " + calorieCaculation.getCaloire());
	}
	
	public static class Builder{
		private int subPosture; 
		private double tsk; 
		private double tsk0; 
		private double ta; 
		private double tr; 
		private double pa; 
		private double va;
		private double walksp; 
		private double icl; 
		private double ap;  
		private double fr;  
		private double theta; 
		private boolean defdir; 
		private boolean defspeed; 
		private double swpMeasured; 
		private double swp0Measured; 
		private double weight; 
		private double height; 
		private double tre;
		private double swp;
		private double tcreq;
		private double tsktcrwg;
		private double met; 
		private CalorieCaculation calorieCaculation;
		
		public Builder(){};

		public synchronized Builder setSubPosture(int subPosture) {
			this.subPosture = subPosture;
			return this;
		}

		public synchronized Builder setTsk(double tsk) {
			this.tsk = tsk;
			return this;
		}

		public synchronized Builder setTsk0(double tsk0) {
			this.tsk0 = tsk0;
			return this;
		}

		public synchronized Builder setTa(double ta) {
			this.ta = ta;
			return this;
		}

		public synchronized Builder setTr(double tr) {
			this.tr = tr;
			return this;
		}

		public synchronized Builder setPa(double pa) {
			this.pa = pa;
			return this;
		}

		public synchronized Builder setVa(double va) {
			this.va = va;
			return this;
		}

		public synchronized Builder setWalksp(double walksp) {
			this.walksp = walksp;
			return this;
		}

		public synchronized Builder setIcl(double icl) {
			this.icl = icl;
			return this;
		}

		public synchronized Builder setAp(double ap) {
			this.ap = ap;
			return this;
		}

		public synchronized Builder setFr(double fr) {
			this.fr = fr;
			return this;
		}

		public synchronized Builder setTheta(double theta) {
			this.theta = theta;
			return this;
		}

		public synchronized Builder setDefdir(boolean defdir) {
			this.defdir = defdir;
			return this;
		}

		public synchronized Builder setDefspeed(boolean defspeed) {
			this.defspeed = defspeed;
			return this;
		}

		public synchronized Builder setSwpMeasured(double swpMeasured) {
			this.swpMeasured = swpMeasured;
			return this;
		}

		public synchronized Builder setSwp0Measured(double swp0Measured) {
			this.swp0Measured = swp0Measured;
			return this;
		}

		public synchronized Builder setWeight(double weight) {
			this.weight = weight;
			return this;
		}

		public synchronized Builder setHeight(double height) {
			this.height = height;
			return this;
		}

		public synchronized Builder setTre(double tre) {
			this.tre = tre;
			return this;
		}

		public synchronized Builder setSwp(double swp) {
			this.swp = swp;
			return this;
		}

		public synchronized Builder setTcreq(double tcreq) {
			this.tcreq = tcreq;
			return this;
		}

		public synchronized Builder setTsktcrwg(double tsktcrwg) {
			this.tsktcrwg = tsktcrwg;
			return this;
		}

		public synchronized Builder setMet(double met) {
			this.met = met;
			return this;
		}
			
		public CalorieCaculation build(){
			calorieCaculation = new CalorieCaculation(this);
			return calorieCaculation;
		}
		
		public synchronized void updateAll(){
			calorieCaculation.updateAll(this);
		}
	}

}
