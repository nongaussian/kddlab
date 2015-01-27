use imdb_small;
/*
drop table results;
drop table experiments;
*/

use imdb_2005_a;
use imdb;
drop table results;
drop table experiments;
create table experiments(
	id integer auto_increment
	,wtype integer /* weight type*/
	,stype integer /* sim type*/
	,opr double/* object pruning ratio*/
	,rpr double/* object pruning ratio*/
	,lr double /* link ratio*/	
	,nIter integer
	,t_sim	integer
	,t_match integer
	,primary key (id)
	
);
create table results(
	exp_id integer
	,vtype integer
	,n_true integer
	,n_false integer
	,n_nomatch integer

	,foreign key (exp_id) references experiments(id) on delete cascade
);

/*
truncate results;
truncate experiments;





truncate movies_directors;
truncate roles;
truncate movies;
truncate actors;
truncate directors;


insert into movies(id,name,rank,year) values (1,'m1',9.0, 2007);
insert into movies(id,name,rank,year) values (2,'m2',8.0, 2011);
insert into movies(id,name,rank,year) values (3,'m3',8.0, 2015);

insert into actors(film_count, first_name,gender,id,last_name) values (2, 'a1','M',1,'a1');
insert into actors(film_count, first_name,gender,id,last_name) values (1, 'a2','M',2,'a2');
insert into actors(film_count, first_name,gender,id,last_name) values (2, 'a3','F',3,'a3');

insert into roles values (1, 1,'11');
insert into roles values (1, 2,'12');
insert into roles values (2, 3,'23');
insert into roles values (3, 2,'32');
insert into roles values (3, 3,'33');

insert into directors (first_name,id,last_name)values('d1',1,'d1');
insert into directors (first_name,id,last_name) values('d2',2,'d2');

insert into movies_directors(director_id,movie_id) values (1,1);
insert into movies_directors(director_id,movie_id) values (2,2);
insert into movies_directors(director_id,movie_id) values (1,3);*/
