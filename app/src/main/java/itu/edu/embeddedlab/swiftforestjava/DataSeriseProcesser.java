package itu.edu.embeddedlab.swiftforestjava;

import java.util.*;


public class DataSeriseProcesser {
	
	
	private static int accThreshhold=0;

    public static Instance convertXYZtoInstance(List<List<Long>> data){
    	double[] features=new double[28];
    	double accavgx=0.0;
    	double accavgy=0.0;
    	double accavgz=0.0;
    	double accmaxx=0.0;
    	double accmaxy=0.0;
    	double accmaxz=0.0;
    	double accminx=0.0;
    	double accminy=0.0;
    	double accminz=0.0;
    	double accabsx=0.0;
    	double accabsy=0.0;
    	double accabsz=0.0;
    	double accres=0.0;
    	double accavgres=0.0;
    	double Dx=0.0;
    	double Dy=0.0;
    	double Dz=0.0;
    	double Da1=0.0;
    	double Da2=0.0;
    	double Da3=0.0;
    	double Db1=0.0;
    	double Db2=0.0;
    	double Db3=0.0;
    	double D1=0.0;
    	double D2=0.0;
    	double D3=0.0;
    	double frex=0.0;
    	double frey=0.0;
    	double frez=0.0;

    	double sumx=0.0;
    	double sumy=0.0;
    	double sumz=0.0;
    	
    	for(List<Long> slice:data){
   		 sumx=sumx+(double)(slice.get(0));
		 sumy=sumy+(double)(slice.get(1));
		 sumz=sumz+(double)(slice.get(2));
    	}
    	accavgx=sumx/data.size();
    	accavgy=sumy/data.size();
    	accavgz=sumz/data.size();
        int i=0;
    	for(i=0;i<(data.size()-1);i++){
        	if ((double)data.get(i).get(0)>accmaxx)
        		accmaxx=(double)data.get(i).get(0);
        	if ((double)data.get(i).get(0)<accminx)
        		accminx=(double)data.get(i).get(0);
        	accabsx=accabsx+Math.abs((double)data.get(i).get(0)-accavgx);
        	if ((((double)data.get(i).get(0)-accThreshhold)>0 &&((double)data.get(i+1).get(0)-accThreshhold)<0) ||(((double)data.get(i).get(0)-accThreshhold)<0 &&((double)data.get(i+1).get(0)-accThreshhold)>0))
        		frex++;
        	
        	if ((double)data.get(i).get(1)>accmaxy)
        		accmaxy=(double)data.get(i).get(1);
        	if ((double)data.get(i).get(1)<accminy)
        		accminy=(double)data.get(i).get(1);
        	accabsy=accabsy+Math.abs((double)data.get(i).get(1)-accavgy);
        	if ((((double)data.get(i).get(1)-accThreshhold)>0 &&((double)data.get(i+1).get(1)-accThreshhold)<0) ||(((double)data.get(i).get(1)-accThreshhold)<0 &&((double)data.get(i+1).get(1)-accThreshhold)>0))
        		frey++;
        	
        	if ((double)data.get(i).get(2)>accmaxz)
        		accmaxz=(double)data.get(i).get(2);
        	if ((double)data.get(i).get(2)<accminz)
        		accminz=(double)data.get(i).get(2);
        	accabsz=accabsz+Math.abs((double)data.get(i).get(2)-accavgz);
        	if ((((double)data.get(i).get(2)-accThreshhold)>0 &&((double)data.get(i+1).get(2)-accThreshhold)<0) ||(((double)data.get(i).get(2)-accThreshhold)<0 &&((double)data.get(i+1).get(2)-accThreshhold)>0))
        		frez++;
        	
        	accres=accres+Math.abs((double)data.get(i+1).get(0)-(double)data.get(i).get(0))+Math.abs((double)data.get(i+1).get(1)-(double)data.get(i).get(1))+Math.abs((double)data.get(i+1).get(2)-(double)data.get(i).get(2));
        		
        }
    	
        if(i==(data.size()-1)){
        	if ((double)data.get(i).get(0)>accmaxx)
        		accmaxx=(double)data.get(i).get(0);
        	if ((double)data.get(i).get(0)<accminx)
        		accminx=(double)data.get(i).get(0);
        	accabsx=accabsx+Math.abs((double)data.get(i).get(0)-accavgx);
        	
        	if ((double)data.get(i).get(1)>accmaxy)
        		accmaxy=(double)data.get(i).get(1);
        	if ((double)data.get(i).get(1)<accminy)
        		accminy=(double)data.get(i).get(1);
        	accabsy=accabsy+Math.abs((double)data.get(i).get(1)-accavgy);
        	
        	if ((double)data.get(i).get(2)>accmaxz)
        		accmaxz=(double)data.get(i).get(2);
        	if ((double)data.get(i).get(2)<accminz)
        		accminz=(double)data.get(i).get(2);
        	accabsz=accabsz+Math.abs((double)data.get(i).get(2)-accavgz);
   
        }
        
        accabsx=accabsx/data.size();
        accabsy=accabsy/data.size();
        accabsz=accabsz/data.size();
        accavgres=accres/(data.size()-1);
        Dx=accmaxx-accminx;
        Dy=accmaxy-accminy;
        Dz=accmaxz-accminz;
        Da1=accavgx-accavgy-accavgz;
        Da2=accavgy-accavgz-accavgx;
        Da3=accavgz-accavgx-accavgy;
        Db1=accabsx-accabsy-accabsz;
        Db2=accabsy-accabsx-accabsz;
        Db3=accabsz-accabsx-accabsy;
        D1=Dx-Dy-Dy;
        D2=Dy-Dz-Dx;
        D3=Dz-Dx-Dy;
        
        
        
    	features[0]=accavgx;
    	features[1]=accavgy;
    	features[2]=accavgz;
    	features[3]=accmaxx;
    	features[4]=accmaxy;
    	features[5]=accmaxz;
    	features[6]=accminx;
    	features[7]=accminy;
    	features[8]=accminz;
    	features[9]=accabsx;
    	features[10]=accabsy;
    	features[11]=accabsz;
    	features[12]=accavgres;
    	features[13]=Dx;
    	features[14]=Dy;
    	features[15]=Dz;
    	features[16]=Da1;
    	features[17]=Da2;
    	features[18]=Da3;
    	features[19]=Db1;
    	features[20]=Db2;
    	features[21]=Db3;
    	features[22]=D1;
    	features[23]=D2;
    	features[24]=D3;
    	features[25]=frex;
    	features[26]=frey;
    	features[27]=frez;
    	
    	
    	Instance instance = new DenseInstance(features, null);
    	return instance;
    }
    

}
