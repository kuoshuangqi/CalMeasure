package itu.edu.embeddedlab.swiftforestjava;

/**
 * This file is part of the Java Machine Learning Library
 * 
 * The Java Machine Learning Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * The Java Machine Learning Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Java Machine Learning Library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Copyright (c) 2006-2009, Thomas Abeel
 * 
 * Project: http://java-ml.sourceforge.net/
 * 
 */
import java.io.*;
import java.util.*;



/**
 * This tutorial show how to use a the k-nearest neighbors classifier.
 * 
 * @author Thomas Abeel
 * 
 */
public class MainOnPhoneTest {
    /**
     * Shows the default usage of the KNN algorithm.
     */
    public static void main(String[] args)throws Exception {

        /* random a raw data list */
    	List<List<Long>> lists=new ArrayList<List<Long>>();
    	List<Long> coList=new ArrayList<Long>();
    	coList.add((Long)((long)Math.random()*500));

    	coList.add((Long)((long)Math.random()*500));

    	coList.add((Long)((long)Math.random()*500));

    	coList.add((Long)((long)Math.random()*500));

    	for(int i=0;i<80;i++){
    		coList.set(0, (Long)((long)Math.random()*500));

    		coList.set(1, (Long)((long)Math.random()*500));

    		coList.set(2, (Long)((long)Math.random()*500));

    		coList.set(3, (Long)((long)Math.random()*500));
    		
    		lists.add(coList);
   	
    		
    	}
    	Instance features=DataSeriseProcesser.convertXYZtoInstance(lists);
    	Classifier forest=RandomForest.loadClassifier("saved_forest.out");
    	Object resulttype=forest.classify(features);
    	System.out.println(resulttype.toString());
    }
    
    
}	 

