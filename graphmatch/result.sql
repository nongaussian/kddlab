use imdb_2005_a;

select mpr, wtype, stype, vtype, mm, count(*),avg(n_true/(n_true+n_false)) as prc from experiments, results
where experiments.id =  results.exp_id
	/*and mpr		=	0.5*/
	and vtype	=	2
	and lr		=	0.1
	and rpr		=	0.1
group by wtype, stype, mpr, vtype, mm;





/*

select vtype, mpr, count(*),avg(n_true/(n_true+n_false)) as prc from experiments, results
where experiments.id =  results.exp_id
	and lr		=	0.2
	and nIter	=	10
	and wtype	=	0
group by vtype, mpr;

select vtype, lr, count(*),avg(n_true/(n_true+n_false)) as prc from experiments, results
where experiments.id =  results.exp_id
	and mpr		=	0.2
	and nIter	=	10
	and wtype	=	0
group by vtype, lr;	

select vtype, lr, count(*),avg(n_true/(n_true+n_false)) as prc from experiments, results
where experiments.id =  results.exp_id
	and mpr		=	0.5
	and nIter	=	10
	and wtype	=	0
group by vtype, lr;	
*/