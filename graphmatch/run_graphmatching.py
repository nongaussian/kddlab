import os

dbname = 'imdb_2005_a'
nMovies_list = [250, 500, 1000]
mpr_list = [0.2]
epr_list = [0.05]
link_ratio_list = [0.05]
numPrunedGraphs = 5


algorithm_list = ['simrank']

#Simrank parameters
nIter = 50
th_converge_list = [1E-6];
wtype = 'npairs'
stypes = ['sr','srkl']


#dbname = 'imdb_small'
#nMovies_list = [100]


for nMovies in nMovies_list:
    for mpr in mpr_list:
        for epr in epr_list:
            for lr in link_ratio_list:
                #Make data
                data_parameters = '-dbname '+ dbname+' -mm '+str(nMovies)
                data_parameters += (' -mpr '+str(mpr)+' -epr '+str(epr)+' -lr '+str(lr))
                
                for gid in range(numPrunedGraphs):
                    for algo in algorithm_list:
                        if algo == 'simrank':
                            for stype in stypes:
                                for th_converge in th_converge_list:               
                                    #Run algorithm 
                                    simrank_parameters =' -algo '+algo+' -wt '+wtype+ ' -st '+stype+' -niter '+str(nIter)+' -thcvg '+ str(th_converge)                         
                                    cmd_str = 'java -jar graphmatching.jar graphmatching '+data_parameters+' -gid '+str(gid)+simrank_parameters
                                    print (cmd_str)
                                    os.system(cmd_str)