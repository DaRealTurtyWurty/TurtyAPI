# format: option_a,votes_a,option_b,votes_b
# this script will go through all lines in the all_unique.csv file and remove any lines that have the same options

import csv
import os

# open the all_unique.csv file
with open('all_unique.csv', 'r') as f:
    reader = csv.reader(f)
    lines = list(reader)

# create a new file to write to
newFile = open('all_unique_no_dupes.csv', 'w')

# create a list to hold the lines that have already been written
writtenLines = []

# go through each line in the all_unique.csv file
for line in lines:
    # if the line is empty, skip it
    if line == [] or line == ['']:
        continue

    # if the line has already been written, skip it
    if line in writtenLines:
        continue

    # if the options have already been written (ignore votes, ignore case, trim whitespace), skip it
    duplicate = False
    for writtenLine in writtenLines:
        if line[0].lower().strip() == writtenLine[0].lower().strip() and line[2].lower().strip() == writtenLine[2].lower().strip():
            duplicate = True
            break

    if duplicate:
        continue
    
    print(line)

    # add the line to the list of written lines
    writtenLines.append(line)

    # if the line has not been written, write it to the new file
    newFile.write(line[0] + ',' + line[1] + ',' + line[2] + ',' + line[3] + '\n')

# close the new file
newFile.close()