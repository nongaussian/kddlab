import os


dbname = 'imdb'
numPrunedGraphs = 5
nIter = 50

nMovies_list = [1000,2000,3000,4000]
mpr_list = [0.1, 0.25, 0.5]
epr_list = [0.05, 0.1];
link_ratio_list = [0.05, 0.1, 0.2 ]
th_converge_list = [1E-6];
wtype = 'npairs'
stypes = ['sr','srkl']


dbname = 'imdb_2005_a'
nMovies_list = [100]#, 250, 500, 1000]
mpr_list = [0.2]
epr_list = [0.05]
link_ratio_list = [0.05]



#dbname = 'imdb_small'
#nMovies_list = [100]





for nMovies in nMovies_list:
    for mpr in mpr_list:
        for epr in epr_list:
            for lr in link_ratio_list:
                #Make data
                cmd_str = 'java -jar graphmatching.jar dataloader '+dbname+' -mm '+str(nMovies)
                cmd_str += (' -mpr '+str(mpr)+' -epr '+str(epr)+' -lr '+str(lr)+' -npg '+str(numPrunedGraphs))
                print (cmd_str)
                os.system(cmd_str)
                  
                for stype in stypes:
                    for th_converge in th_converge_list:
                        for gid in range(numPrunedGraphs):
                            #Run algorithm                           
                            cmd_str = 'java -jar graphmatching.jar graphmatching -dbname '+dbname+' -mm '+str(nMovies)
                            cmd_str += (' -mpr '+str(mpr)+' -epr '+str(epr)+' -lr '+str(lr)+' -wt '+wtype+ ' -st '+stype)
                            cmd_str += (' -niter '+str(nIter)+' -thcvg '+ str(th_converge)+' -gid '+str(gid))
                            print (cmd_str)
                            os.system(cmd_str)
        