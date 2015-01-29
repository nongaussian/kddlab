use imdb;


drop database if exists imdb_2005_a;

create database imdb_2005_a;
use imdb_2005_a;
create table movies as
	select * from imdb.movies
	where year >=2005;

create table actors as
	select A.id, film_count, first_name, gender, last_name from imdb.actors as A, imdb.roles as R, movies
	where   R.actor_id = A.id
		and R.movie_id = movies.id
	group by A.id;

create table roles as
	select R.actor_id, R.movie_id, R.role from actors, imdb.roles as R, movies
	where   R.actor_id = actors.id
		and R.movie_id = movies.id;

create table directors as
	select D.id, first_name, last_name from imdb.directors as D, imdb.movies_directors as MD, movies
	where   MD.director_id = D.id
		and MD.movie_id = movies.id
	group by D.id;
create table movies_directors as
	select MD.director_id, MD.movie_id from directors , imdb.movies_directors as MD, movies
	where   MD.director_id = directors.id
		and MD.movie_id = movies.id;
	

