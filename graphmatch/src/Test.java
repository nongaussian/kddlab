import gm.graph.Graph;

import java.awt.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


public class Test {
	public static void main(String[] arvs){
		//String[] args = {"imdb", "-epr", "0.1", "-mpr", "0.2"};
		//String[] args = {"-h"};
		//argtest(args);
		
		SerializationProxyTest();
		
	}
	
	
	public static void argtest(String[] args){
		 ArgumentParser parser = ArgumentParsers.newArgumentParser("GraphMatching")
	                .description("Graph matching.");
	        parser.addArgument("dbname")
	                //.metavar("-d")
	                .type(String.class)
	                .help("Database name");
	        parser.addArgument("-mpr")
	        		.type(Double.class)
	        		.required(true)
	        		.help("movie prunining ratio");
	        parser.addArgument("-epr")
	        		.type(Double.class)
	        		.required(true)
	        		.help("edge prunining ratio");
	        parser.addArgument("-lr")
	        		.type(Double.class)
	        		.setDefault(0.1)
	        		.help("known link ratio");
	        parser.addArgument("-st")
	        		.type(String.class)
	        		.setDefault("npairs")
	        		.help(String.format("Similarity measures: npairs, uniform\n" +
	        				"\t- npairs: w_i is proportional to |I(u)|*|I(v)|\n" +
	        				"\t- uniform: w_i following a uniform distribution"));
	        parser.addArgument("-mm","--maxMovies")
	        		.type(Integer.class)
	        		.nargs("?")
	        		.setDefault(-1)
	        		.help("Maximum number of movies");
	        parser.addArgument("-niter","--nIter")
			        .type(Integer.class)
			        .nargs("?")
			        .setDefault(10)
	        .help("Number of Iterations");
	        try {
	            Namespace res = parser.parseArgs(args);
	            System.out.println(res);
	        } catch (ArgumentParserException e) {
	            parser.handleError(e);
	        }
	}
	
	public static void readFileTest(){
		try {
			readFile("data/imdb/actors.list", "ACTORS", 100);
			System.out.println("**************************************************************************");
			System.out.println();
			readFile("data/imdb/actresses.list", "ACTRESSES", 100);
			System.out.println("**************************************************************************");
			System.out.println();
			readFile("data/imdb/business.list", "BUSINESS", 1000);
			System.out.println("**************************************************************************");
			System.out.println();
			readFile("data/imdb/movies.list", "MOVIES", 1000);
			System.out.println("**************************************************************************");
			System.out.println();
			readFile("data/imdb/writers.list", "WRITERS", 100);
			System.out.println();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void readFile(String filename, String name, int lines) throws IOException{
		File f = new File(filename);
		BufferedReader br = new BufferedReader( new FileReader(f) );
		int l = 0;
		String line;
		boolean skip  = name.length()>0;
		
		while((line=br.readLine())!=null){
			if(skip){
				if(line.contains(name+" LIST")){
					skip = false;
				}else{
					continue;
				}
			}
			
			if(++l>lines){
				break;
			}
			System.out.println(line);
		}
		br.close();
	}

	
	
	public static void SerializationProxyTest(){
		Person[] people = new Person[2];
		people[0] = new Person("1", "1", 30);
		people[1] = new Person("2", "2", 31);
		
		people[0].setSpouse(people[1]);
		people[1].setSpouse(people[0]);
		try{
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("tmp.data")));
			out.writeObject(people);
			out.close();
			
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("tmp.data")));
			Person[] people2 = (Person[]) in.readObject();
			System.out.println(people2[0].toString());
			System.out.println(people2[1].toString());
			in.close();
		}catch(IOException | ClassNotFoundException e){
			e.printStackTrace();
		}
		System.out.println(people[0].toString());
		System.out.println(people[1].toString());
		
	}
}

class PersonProxy implements java.io.Serializable{
	public PersonProxy(Person orig)
	{
	    data = orig.getFirstName() + "," + orig.getLastName() + "," + orig.getAge();
	    if (orig.getSpouse() != null)
	    {
	        Person spouse = orig.getSpouse();
	        data = data + "," + spouse.getFirstName() + "," + spouse.getLastName() + ","  
	          + spouse.getAge();
	    }
	}
	
	public String data;
	private Object readResolve()
	    throws java.io.ObjectStreamException
	{
	    String[] pieces = data.split(",");
	    Person result = new Person(pieces[0], pieces[1], Integer.parseInt(pieces[2]));
	    if (pieces.length > 3)
	    {
	        result.setSpouse(new Person(pieces[3], pieces[4], Integer.parseInt
	          (pieces[5])));
	        result.getSpouse().setSpouse(result);
	    }
	    return result;
	}
}

class Person implements java.io.Serializable{
	public Person(String fn, String ln, int a)
	{
	    this.firstName = fn; this.lastName = ln; this.age = a;
	}
	
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public int getAge() { return age; }
	public Person getSpouse() { return spouse; }
	
	private Object writeReplace()
	    throws java.io.ObjectStreamException
	{
	    return new PersonProxy(this);
	}
	
	public void setFirstName(String value) { firstName = value; }
	public void setLastName(String value) { lastName = value; }
	public void setAge(int value) { age = value; }
	public void setSpouse(Person value) { spouse = value; }   
	
	public String toString()
	{
	    return "[Person: firstName=" + firstName + 
	        " lastName=" + lastName +
	        " age=" + age +
	        " spouse=" + spouse.getFirstName() +
	        "]";
	}    
	
	private String firstName;
	private String lastName;
	private int age;
	private Person spouse;
}


