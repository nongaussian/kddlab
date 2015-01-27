import os

dbname = 'imdb_2005_a'          #database name
nMovies_list = [250, 500, 1000] #Maximum number of
mpr_list = [0.2]                #Movie pruning ratio
epr_list = [0.05]               #Edge pruning ratio
link_ratio_list = [0.05]        #Known link ratio
numPrunedGraphs = 5             #Number of modified graphs for an original graph


algorithm_list = ['simrank']    #Algorithm name

#Simrank parameters
nIter = 50                      #Maximum number of iterations
th_converge_list = [1E-6];      #Threshold of simrank convergence
wtype = 'npairs'                #vertex weight type (w_i)   npairs: proportional to |I(u)|*|I(v)|, uniform: uniform
stypes = ['sr','srkl']          #simrank type    sr: original simrank    srkl: modified version


#dbname = 'imdb_small'
#nMovies_list = [100]


for nMovies in nMovies_list:
    for mpr in mpr_list:
        for epr in epr_list:
            for lr in link_ratio_list:
                #Make data
                data_parameters = '-dbname '+ dbname+' -mm '+str(nMovies)
                data_parameters += (' -mpr '+str(mpr)+' -epr '+str(epr)+' -lr '+str(lr))
                cmd_str = 'java -jar graphmatching.jar dataloader ' + data_parameters+' -npg '+str(numPrunedGraphs)
                print (cmd_str)
                os.system(cmd_str)
                
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
            