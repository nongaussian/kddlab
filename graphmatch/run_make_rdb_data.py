import os

dbname = 'imdb_2005_a'          #database name
nMovies_list = [1000] #Maximum number of
mpr_list = [0.2]                #Movie pruning ratio
epr_list = [0.05]               #Edge pruning ratio
link_ratio_list = [0.05]        #Known link ratio
numPrunedGraphs = 5             #Number of modified graphs for an original graph




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
                
                