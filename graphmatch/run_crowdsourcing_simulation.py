import os

nIter = 20

dbname = 'imdb'
nMovies_list = [1000,2000,3000,4000]
mpr_list = [0.1, 0.25, 0.5]
epr_list = [0.05, 0.1];
link_ratio_list = [0.05, 0.1, 0.2 ]
th_converge_list = [1.0*10E-6]
wtypes = ['npairs','uniform']
repetition = 5
matching_th_list = [0.8, 0.9, 0.95]

dbname = 'imdb_2005_a'
nMovies_list = [200, 500, 1000]
mpr_list = [0.1]
epr_list = [0.05]
link_ratio_list = [0.03]
stypes = ['sr','srkl']
wtype = 'npairs'
#nIter_list = [10]
repetition = 1





for nMovies in nMovies_list:
    for mpr in mpr_list:
        for epr in epr_list:
            for lr in link_ratio_list:
                for stype in stypes:
                    for th_converge in th_converge_list:
                        for matching_th in matching_th_list:
                            for r in range(repetition):
                                cmd_str = 'java -jar graphmatching_crowdsourcing_simulation.jar '
                                cmd_str += (dbname+' -mm '+str(nMovies))
                                cmd_str += (' -mpr '+str(mpr)+' -epr '+str(epr)+' -lr '+str(lr)+' -wt '+wtype+ ' -st '+stype)
                                cmd_str += (' -niter '+str(nIter)+' -thcvg '+ str(th_converge)+' -thm '+str(matching_th))
                                print (cmd_str+ '  r:'+str(r))
                                os.system(cmd_str)
            