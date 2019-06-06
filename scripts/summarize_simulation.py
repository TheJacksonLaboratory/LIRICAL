import sys

if (len(sys.argv)<2):
    print("usage python summarize_simulation.py ../vcf_simulation_results.tst")
    exit(1)

fname=sys.argv[1]
print("parsing file ",fname)


counts={}
total=0;

with open(fname,'r') as f:
    for line in f:
        if line.startswith('#'):
            continue; ## header
        fields=line.split('\t')
        rank=fields[0]
        total += 1
        if rank in counts:
            c = counts[rank]
            counts[rank]=c+1
        else:
            counts[rank]=1


cumulative_percentage=0
count=0;

for x in range(1,1000):
    s = str(x)
    if s in counts:
        v = counts[s]
        count += v
        perc = 100*v/total
        cumulative_percentage += perc
        print("Rank {:>2}: {:>3} times ({:.2f}% - cumulative {:.2f}%)".format(s,v,perc,cumulative_percentage))


print("total count ",count)
