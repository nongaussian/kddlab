import os

nIter = 50

dbname = 'imdb'
nMovies_list = [1000,2000,3000,4000];
mpr_list = [0.1, 0.25, 0.5]
epr_list = [0.05, 0.1];
link_ratio_list = [0.05, 0.1, 0.2 ]
th_converge_list = [1E-6];
#wtypes = ['npairs','uniform']
wtype = 'npairs'


repetition = 3

dbname = 'imdb_2005_a'
nMovies_list = [50, 100, 250, 500, 1000];
#th_converge_list = [1E-8];
mpr_list = [0.2]
epr_list = [0.05]
link_ratio_list = [0.05]
stypes = ['sr','srkl']






for nMovies in nMovies_list:
    for mpr in mpr_list:
        for epr in epr_list:
            for lr in link_ratio_list:
                for stype in stypes:
                    for th_converge in th_converge_list:
                        for r in range(repetition):
                            cmd_str = 'java -jar graphmatching.jar '+dbname+' -mm '+str(nMovies)
                            cmd_str += (' -mpr '+str(mpr)+' -epr '+str(epr)+' -lr '+str(lr)+' -wt '+wtype+ ' -st '+stype)
                            cmd_str += (' -niter '+str(nIter)+' -thcvg '+ str(th_converge))
                            print (cmd_str+ '  r:'+str(r))
                            os.system(cmd_str)
        