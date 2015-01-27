package gm.graph;

import gm.data.SqlUnit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class Graph implements Serializable{
	public static final int VERTEX_TYPE_MOVIE = 0;
	public static final int VERTEX_TYPE_ACTOR = 1;
	public static final int VERTEX_TYPE_DIRECTOR = 2;
	
	public static Random r = new Random();
	
	
	HashMap<Integer,Integer> movieIDs = new HashMap<Integer, Integer>();
	HashMap<Integer,Integer> actorIDs = new HashMap<Integer, Integer>();
	HashMap<Integer,Integer> directorIDs = new HashMap<Integer, Integer>();
	//SqlUnit sqlunit;
	
	private Movie[] movies;
	private Actor[] actors;
	private Director[] directors;
	
	
	private String graphname;
	
	//Read graph from sql
	public Graph(SqlUnit sqlunit, int maxMovies){
		loadMovies(sqlunit, maxMovies);
		loadActors(sqlunit);
		loadDirectors(sqlunit);
		
		loadActings(sqlunit);
		loadDirectings(sqlunit);
		removeSingletonVertices();
		
		graphname = String.format("g_%s_mm%d", sqlunit.dbname, maxMovies);
	}
	
	//Make noised graph
	public Graph(Graph g, SqlUnit sqlunit, double movie_pruning_ratio, double relation_pruning_ratio, double link_ratio){
		//sqlunit = g.sqlunit;
		double MOVIE_PRUNING_RATIO = movie_pruning_ratio;
		double RELATION_PRUNING_RATIO = relation_pruning_ratio;
		
		
		ArrayList<Movie> movie_list = new ArrayList<Movie>(); 
		for(Movie m:g.getMovies()){
			if(r.nextDouble()>MOVIE_PRUNING_RATIO){movie_list.add(new Movie(m));}
		}
		Collections.shuffle(movie_list);
		movies = movie_list.toArray(new Movie[0] );
		setMovieIds();
		
		System.out.printf("#ofMovies g: %d g': %d\n",g.getMovies().length, getMovies().length);
		ArrayList<Actor> actor_list = new ArrayList<Actor>(); 
		for(Actor actor:g.getActors()){
			actor_list.add(new Actor(actor));
		}
		Collections.shuffle(actor_list);
		actors = actor_list.toArray(new Actor[0]);
		setActorIds();
		
		System.out.printf("#ofActors g: %d g': %d\n",g.getActors().length,getActors().length);
		ArrayList<Director> director_list = new ArrayList<Director>(); 
		for(Director director:g.getDirectors()){
			director_list.add(new Director(director));
			
		}
		Collections.shuffle(director_list);
		directors = director_list.toArray(new Director[0]);
		setDirectorIds();
		System.out.printf("#ofDirectors g: %d g': %d\n",g.getDirectors().length,getDirectors().length);
		
		loadDirectings(sqlunit, RELATION_PRUNING_RATIO);
		loadActings(sqlunit, RELATION_PRUNING_RATIO);
		removeSingletonVertices();
		setKnownLinksRandom(g, this, link_ratio);
		
		this.graphname = g.graphName() +String.format("_mpr%s_rpr%s_lr%s", Double.toString(movie_pruning_ratio).replace(".", "_"), Double.toString(relation_pruning_ratio).replace(".", "_"), Double.toString(link_ratio).replace(".", "_"));
		
	}
	
	//Make graph using data arrays
	public Graph (String graphname, Movie[] movies, Actor[] actors, Director[] directors){
		this.graphname = graphname;
		this.movies = movies;
		this.actors = actors;
		this.directors = directors;
		setMovieIds();
		setActorIds();
		setDirectorIds();
	}
	
	private static void setKnownLinksRandom(Graph g0, Graph g1, double ratio){
		if (ratio>1.0){
			System.out.println("Ratio should be less than 1.0");
			System.exit(0);
		}
		
		HashMap<Integer,Integer> Idmap0 = g0.getIdMap(VERTEX_TYPE_MOVIE);
		Movie[] movies0 = g0.getMovies();
		Movie[] movies1 = g1.getMovies();
		int nLinkes = (int)(ratio*movies1.length);
		
		System.out.println("Make "+nLinkes+" links (movie)");
		int count_links = 0;
		while(count_links<nLinkes){
			int next_id1 = r.nextInt(movies1.length);
			if(movies1[next_id1].getKnownLink()==null){
				Vertex v1 = movies1[next_id1];
				Vertex v0 = movies0[Idmap0.get(v1.getOriginalId())];
				v0.setKnownLink(v1);
				v1.setKnownLink(v0);
				count_links++;
			}
		}
		
		Idmap0 = g0.getIdMap(VERTEX_TYPE_ACTOR);
		Actor[] actors0 = g0.getActors();
		Actor[] actors1 = g1.getActors();
		nLinkes = (int)(ratio*actors1.length);
		
		System.out.println("Make "+nLinkes+" links (actor)");
		count_links = 0;
		while(count_links<nLinkes){
			int next_id1 = r.nextInt(actors1.length);
			if(actors1[next_id1].getKnownLink()==null){
				Vertex v1 = actors1[next_id1];
				Vertex v0 = actors0[Idmap0.get(v1.getOriginalId())];
				v0.setKnownLink(v1);
				v1.setKnownLink(v0);
				count_links++;
			}
		}
		
		Idmap0 = g0.getIdMap(VERTEX_TYPE_DIRECTOR);
		Director[] directors0 = g0.getDirectors();
		Director[] directors1 = g1.getDirectors();
		nLinkes = (int)(ratio*directors1.length);
		
		System.out.println("Make "+nLinkes+" links (director)");
		count_links = 0;
		while(count_links<nLinkes){
			int next_id1 = r.nextInt(directors1.length);
			if(directors1[next_id1].getKnownLink()==null){
				Vertex v1 = directors1[next_id1];
				Vertex v0 = directors0[Idmap0.get(v1.getOriginalId())];
				v0.setKnownLink(v1);
				v1.setKnownLink(v0);
				count_links++;
			}
		}
	}
	
	
	
	public void loadMovies(SqlUnit sqlunit, int maxMovies){
		ResultSet rs = sqlunit.executeQuery("select movies.id,movies.name, movies.year, movies.rank, count(*) as count " +
				"from movies, roles	where movies.id = roles.movie_id group by movies.id;");
		ArrayList<Movie> m_list = new ArrayList<Movie>();
		int mcount = 0;
		try {
			rs.beforeFirst();
			while(rs.next()){
				if(rs.getInt("count")<5)
					continue;
				m_list.add(new Movie(rs.getInt("id"), rs.getString("name"), rs.getInt("year")));
				if(maxMovies>0&&++mcount>=maxMovies){
					break;
				}
			}
			rs.close();
		} catch (SQLException e) {
			SqlUnit.handlingSQLException(e);
		}
		movies =m_list.toArray(new Movie[0]);
		
		setMovieIds();
		
	}
	
	public void loadActors(SqlUnit sqlunit){
		ResultSet rs = sqlunit.executeQuery("select * from actors;");
		ArrayList<Actor> a_list = new ArrayList<Actor>();
		try {
			rs.beforeFirst();
			while(rs.next()){
				a_list.add(new Actor(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("gender").charAt(0)));
			}
		} catch (SQLException e) {
			SqlUnit.handlingSQLException(e);
		}
		actors =a_list.toArray(new Actor[0]);
		
		setActorIds();
		
	}
	
	public void loadDirectors(SqlUnit sqlunit){
		ResultSet rs = sqlunit.executeQuery("select * from directors;");
		ArrayList<Director> d_list = new ArrayList<Director>();
		try {
			rs.beforeFirst();
			while(rs.next()){
				d_list.add(new Director(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name")));
			}
		} catch (SQLException e) {
			SqlUnit.handlingSQLException(e);
		}

		directors =d_list.toArray(new Director[0]);
		
		setDirectorIds();
		
	}
	
	private static void setIdsMap(HashMap<Integer,Integer> map, Vertex[] vertices){
		for(int m =0; m<vertices.length; m++){
			map.put(vertices[m].getOriginalId(), m);
			vertices[m].setId(m);
		}
	}
	
	private void setMovieIds(){setIdsMap(movieIDs, movies);}
	private void setActorIds(){setIdsMap(actorIDs, actors);}
	private void setDirectorIds(){
		setIdsMap(directorIDs, directors);
	}
	
	
	
	public void loadActings(SqlUnit sqlunit){
		loadActings(sqlunit, 0.0);
	}
	
	public void loadActings(SqlUnit sqlunit, double pruning_ratio){
		ResultSet rs = sqlunit.executeQuery("select * from roles;");
		try {
			rs.beforeFirst();
			while(rs.next()){
				if(pruning_ratio>0.0&& r.nextDouble()<pruning_ratio){
					continue;
				}
				Integer actor_id = actorIDs.get(rs.getInt("actor_id"));
				Integer movie_id = movieIDs.get(rs.getInt("movie_id"));
				if(actor_id==null||movie_id==null){
					continue;
				}
				actors[actor_id].addActing(movies[movie_id]);
				movies[movie_id].addActor(actors[actor_id]);
			}
		} catch (SQLException e) {
			SqlUnit.handlingSQLException(e);
		}
		/*
		for(int a =0;a<actors.length; a++){
			Collections.sort(actors[a].getMovies());
		}
		for(int m =0; m<movies.length; m++){
			Collections.sort(movies[m].getActors());
		}*/
	}
	
	
	public void loadDirectings(SqlUnit sqlunit){
		loadDirectings(sqlunit, 0.0);
	}
	
	public void loadDirectings(SqlUnit sqlunit, double pruning_ratio){
		ResultSet rs = sqlunit.executeQuery("select * from movies_directors;");
		try {
			rs.beforeFirst();
			while(rs.next()){
				if(pruning_ratio>0.0&& r.nextDouble()<pruning_ratio){
					continue;
				}
				Integer director_id = directorIDs.get(rs.getInt("director_id"));
				Integer movie_id = movieIDs.get(rs.getInt("movie_id"));
				if(director_id==null||movie_id==null){
					continue;
				}
				directors[director_id].addDirectedMovie(movies[movie_id]);
				movies[movie_id].addDirector(directors[director_id]);
			}
		} catch (SQLException e) {
			SqlUnit.handlingSQLException(e);
		}
		/*
		for(int d =0;d<directors.length; d++){
			Collections.sort(directors[d].getMovies());
		}
		for(int m =0; m<movies.length; m++){
			Collections.sort(movies[m].getDirectors());
		}*/
	}
	
	
	public void removeSingletonVertices(){
		System.out.println("remove singletons");
		ArrayList<Movie> movies_new  = new ArrayList<Movie>();
		
		for(Movie m: getMovies()){
			if(m.getActors().size()>0||m.getDirectors().size()>0){
				movies_new.add(m);
			}
		}
		if (movies.length>movies_new.size()){
			movieIDs.clear();
			movies = new Movie[movies_new.size()];
			for(int m = 0; m <movies_new.size(); m++){
				movies[m] = movies_new.get(m);
				movies[m].setId(m);
				movieIDs.put(movies[m].getOriginalId(), m);
			}
		}
		
		System.out.printf("# of movies: %d\n", movies.length);
		
		ArrayList<Actor> actors_new  = new ArrayList<Actor>();
		for(Actor a: getActors()){
			if(a.getMovies().size()>0){
				actors_new.add(a);
			}
		}
		if (actors.length>actors_new.size()){
			actorIDs.clear();
			actors = new Actor[actors_new.size()];
			for(int a = 0; a <actors_new.size(); a++){
				actors[a] = actors_new.get(a);
				actors[a].setId(a);
				actorIDs.put(actors[a].getOriginalId(), a);
			}
		}
		System.out.printf("# of actors: %d\n", actors.length);
		
		ArrayList<Director> directors_new  = new ArrayList<Director>();
		for(Director d: getDirectors()){
			if(d.getMovies().size()>0){
				directors_new.add(d);
			}
		}
		if (directors.length>directors_new.size()){
			directorIDs.clear();
			directors = new Director[directors_new.size()];
			for(int d = 0; d <directors_new.size(); d++){
				directors[d] = directors_new.get(d);
				directors[d].setId(d);
				directorIDs.put(directors[d].getOriginalId(), d);
			}
		}
		System.out.printf("# of directors: %d\n", directors.length);
	}
	
	public Movie[] getMovies(){return movies;}
	public Actor[] getActors(){return actors;}
	public Director[] getDirectors(){return directors;}
	
	public HashMap<Integer,Integer> getIdMap(int type){
		switch(type){
		case VERTEX_TYPE_MOVIE:
			return movieIDs;
		case VERTEX_TYPE_ACTOR:
			return actorIDs;
		case VERTEX_TYPE_DIRECTOR:
			return directorIDs;
		}
		return null;
	}
	
	public Vertex get(int id, int type){
		switch(type){
		case VERTEX_TYPE_MOVIE:
			return movies[id];
		case VERTEX_TYPE_ACTOR:
			return actors[id];
		case VERTEX_TYPE_DIRECTOR:
			return directors[id];
		}
		return null;
	}
	
	
	
	
	public static Graph readGraph(String graphname){
		try {
			BufferedReader in = new BufferedReader(new FileReader(getObjPath(graphname)));
			String line = in.readLine();
			String[] header = line.split("\t");
			
			int nMovies = Integer.parseInt(header[2]);
			int nActors = Integer.parseInt(header[3]);
			int nDirectors = Integer.parseInt(header[4]);
			
			// Read Movies
			Movie[] movies = new Movie[nMovies];
			int idx = 0;
			boolean read = false;
			while((line = in.readLine()).length()>0){
				if(!read){
					if(line.startsWith("Movies")){
						read = true;
					}
					continue;
				}
				movies[idx] = new Movie(line);
				idx++;
			}
			
			//Read Actors
			Actor[] actors = new Actor[nActors];

			idx = 0;
			String[] actor_info;
			String[] links;
			read = false;
			while((line = in.readLine()).length()>0){
				if(!read){
					if(line.startsWith("Actors")){
						read = true;
					}
					continue;
				}
				actor_info = line.split(" movies: ");
				actors[idx] = new Actor(actor_info[0]);
				links = actor_info[1].split("\t");
				for(int i = 0; i<links.length; i++){
					int m_id = Integer.parseInt(links[i].split("/")[0]);
					actors[idx].addActing(movies[m_id]);
					movies[m_id].addActor(actors[idx]);
				}
				idx++;
			}
			
			//Read Directors
			Director[] directors = new Director[nDirectors];
			idx = 0;
			String[] director_info;
			read = false;
			while((line = in.readLine())!=null){
				if(!read){
					if(line.startsWith("Directors")){
						read = true;
					}
					continue;
				}
				director_info = line.split(" movies: ");
				directors[idx] = new Director(director_info[0]);
				links = director_info[1].split("\t");
				for(int i = 0; i<links.length; i++){
					int m_id = Integer.parseInt(links[i].split("/")[0]);
					directors[idx].addDirectedMovie(movies[m_id]);
					movies[m_id].addDirector(directors[idx]);
				}
				idx++;
			}
			
			
			in.close();
			
			Graph g = new Graph(graphname, movies, actors, directors);
			return g;
		} catch ( IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}
	public void writeGraph(String graphname){
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(getObjPath(graphname)));
			br.write("Graph\t"+graphname+"\t"+movies.length+"\t"+actors.length+"\t"+directors.length);
			br.newLine();
			
			br.write("Movies="+movies.length);
			br.newLine();
			for(Movie m: movies){
				br.write(m.toString());
				br.newLine();
			}
			
			br.newLine();
			br.write("Actors="+actors.length);
			br.newLine();
			for(Actor a: actors){
				br.write(a.toString()+" movies: "+a.getMoviesIdString());
				br.newLine();
				
			}
			br.newLine();
			br.write("Directors");
			br.newLine();
			for(Director d: directors){
				br.write(d.toString()+" movies: "+d.getMoviesIdString());
				br.newLine();
			}
			
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	@Deprecated
	public void writeGraphText(String graphname){
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(getTextPath(graphname)));
			br.write("Graph "+graphname);
			br.newLine();
			
			br.write("Movies="+movies.length);
			br.newLine();
			for(Movie m: movies){
				br.write(m.toString()+" directors: "+m.getDirectorsIdString()+" actors: "+m.getActorsIdString() );
				br.newLine();
			}
			
			br.newLine();
			br.write("Actors="+movies.length);
			br.newLine();
			for(Actor a: actors){
				br.write(a.toString()+" movies: "+a.getMoviesIdString() );
				br.newLine();
			}
			br.newLine();
			br.write("Directors");
			br.newLine();
			for(Director d: directors){
				br.write(d.toString()+" movies: "+d.getMoviesIdString() );
				br.newLine();
			}
			
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	public String graphName(){
		return graphname;
	}
	
	public static String getObjPath(String graphname){
		return "data/graph/"+graphname+".graph";
	}
	public static String getKnownLinkPath(String graphname){
		return "data/graph/"+graphname+".kl";
	}
	
	@Deprecated
	public static String getTextPath(String graphname){
		return "data/graph/"+graphname+".txt";
	}
	
	public void writeKnownLinks(int gid){
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(getKnownLinkPath(graphName()+"_"+gid)));
			br.write("KnownLinks\t"+graphname);
			br.newLine();
			
			br.write("Movies");
			br.newLine();
			for(Movie m: movies){
				Vertex v = m.getKnownLink();
				if(v!=null){
					br.write(m.getId()+"-"+v.getId());
					br.newLine();
				}
			}
			br.newLine();
			br.write("Actors");
			br.newLine();
			for(Actor a: actors){
				Vertex v = a.getKnownLink();
				if(v!=null){
					br.write(a.getId()+"-"+v.getId());
					br.newLine();
				}
			}
			br.newLine();
			br.write("Directors");
			br.newLine();
			for(Director d: directors){
				Vertex v = d.getKnownLink();
				if(v!=null){
					br.write(d.getId()+"-"+v.getId());
					br.newLine();
				}
			}
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	public void clearKnownLink(){
		for(Movie m: movies){
			m.setKnownLink(null);
		}
		for(Actor a: actors){
			a.setKnownLink(null);
		}
		for(Director d: directors){
			d.setKnownLink(null);
		}
	}
	
}
