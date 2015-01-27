package gm.graph;

import gm.sql.SqlUnit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class Graph {
	public static final int VERTEX_TYPE_MOVIE = 0;
	public static final int VERTEX_TYPE_ACTOR = 1;
	public static final int VERTEX_TYPE_DIRECTOR = 2;
	
	public static Random r = new Random();
	HashMap<Integer,Integer> movieIDs = new HashMap<Integer, Integer>();
	HashMap<Integer,Integer> actorIDs = new HashMap<Integer, Integer>();
	HashMap<Integer,Integer> directorIDs = new HashMap<Integer, Integer>();
	SqlUnit sqlunit;
	
	private Movie[] movies;
	private Actor[] actors;
	private Director[] directors;
	
	public Graph(SqlUnit sqlunit, int maxMovies){
		this.sqlunit = sqlunit;
		movies = loadMovies(maxMovies);
		actors = loadActors();
		directors = loadDirectors();
		
		loadActings();
		loadDirectings();
		removeSingletonVertices();
		
	}
	
	public Graph(Graph g, double movie_pruning_ratio, double relation_pruning_ratio, double link_ratio){
		sqlunit = g.sqlunit;
		double MOVIE_PRUNING_RATIO = movie_pruning_ratio;
		double RELATION_PRUNING_RATIO = relation_pruning_ratio;
		
		
		ArrayList<Movie> movie_list = new ArrayList<Movie>(); 
		for(Movie m:g.getMovies()){
			if(r.nextDouble()>MOVIE_PRUNING_RATIO){movie_list.add(new Movie(m));}
		}
		Collections.shuffle(movie_list);
		movies = movie_list.toArray(new Movie[0] );
		for(int m =0; m<movies.length; m++){
			movieIDs.put(movies[m].getOriginalId(), m);
			movies[m].setId(m);
		}
		
		
		
		System.out.printf("#ofMovies g: %d g': %d\n",g.getMovies().length, getMovies().length);
		ArrayList<Actor> actor_list = new ArrayList<Actor>(); 
		for(Actor actor:g.getActors()){
			actor_list.add(new Actor(actor));
		}
		Collections.shuffle(actor_list);
		actors = actor_list.toArray(new Actor[0]);
		for(int a =0;a<actors.length; a++){
			actorIDs.put(actors[a].getOriginalId(), a);
			actors[a].setId(a);
		}
		
		System.out.printf("#ofActors g: %d g': %d\n",g.getActors().length,getActors().length);
		ArrayList<Director> director_list = new ArrayList<Director>(); 
		for(Director director:g.getDirectors()){
			director_list.add(new Director(director));
			
		}
		Collections.shuffle(director_list);
		directors = director_list.toArray(new Director[0]);
		for(int d =0;d<directors.length; d++){
			directorIDs.put(directors[d].getOriginalId(), d);
			directors[d].setId(d);
		}
		System.out.printf("#ofDirectors g: %d g': %d\n",g.getDirectors().length,getDirectors().length);
		
		loadDirectings(RELATION_PRUNING_RATIO);
		loadActings(RELATION_PRUNING_RATIO);
		removeSingletonVertices();
		setKnownLinksRandom(g, this, link_ratio);
	}
	public static void setKnownLinksRandom(Graph g0, Graph g1, double ratio){
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
	public static void setKnownLinkesTest(Graph g0, Graph g1){
		Movie m0 = g0.movies[g0.movieIDs.get(1)];
		Movie m1 = g1.movies[g1.movieIDs.get(1)];
		m0.setKnownLink(m1);
		m1.setKnownLink(m0);
		
		int count_links = 0;
		int TH_LINKES = 1;
		while(count_links<TH_LINKES){
			count_links++;
		}
	}


	
	public Movie[] loadMovies(int maxMovies){
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
		Movie[] movies =m_list.toArray(new Movie[0]);
		
		for(int m =0; m<movies.length; m++){
			movieIDs.put(movies[m].getOriginalId(), m);
			movies[m].setId(m);
		}
		return movies;
	}
	public Actor[] loadActors(){
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
		Actor[] actors =a_list.toArray(new Actor[0]);
		
		for(int a =0;a<actors.length; a++){
			actorIDs.put(actors[a].getOriginalId(), a);
			actors[a].setId(a);
		}
		return actors;
	}
	public Director[] loadDirectors(){
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

		Director[] directors =d_list.toArray(new Director[0]);
		
		
		for(int d =0;d<directors.length; d++){
			directorIDs.put(directors[d].getOriginalId(), d);
			directors[d].setId(d);
		}
		return directors;
	}
	
	public void loadActings(){
		loadActings(0.0);
	}
	public void loadActings(double pruning_ratio){
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
	
	
	public void loadDirectings(){
		loadDirectings(0.0);
	}
	public void loadDirectings(double pruning_ratio){
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
	
	
}
