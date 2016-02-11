# See Google doc "Zambia Experiment 03"
# plot the mean and 5- and 95-percentiles of lead times by week as
# is the lead time simulation model 

rm(list = ls())

require(plyr)
require(dplyr)

TIMESTEPS_IN_YEAR <- 48

x <- read.table('java.txt', sep = ',', header = TRUE) %>%
    mutate(timestep_of_year = timestep %% TIMESTEPS_IN_YEAR)

y <- ddply(x, .(timestep_of_year), summarise,
                 mean = mean(lead_time),
                 perc05 = round(quantile(lead_time, 0.05)),
                 perc95 = round(quantile(lead_time, 0.95)))

# for plotting purposes, repeat the first row at the end, but change
# the timestep value to the maximum value, i.e., TIMESTEPS_IN_YEAR
first_row <- y[1,]
first_row$timestep_of_year <- TIMESTEPS_IN_YEAR
y <- rbind(y, first_row)
print(tbl_df(y))

# POSIXct stores date-times as the number of seconds since Jan 1, 1970
start.date <- as.POSIXct("2009-01-01 00:00:00", tz='GMT')
number.seq <- TIMESTEPS_IN_YEAR
datetime.seq <- start.date + 
    365 * 24 * 60 * 60 * (0:number.seq) / TIMESTEPS_IN_YEAR

datetime.seq.string <- sapply(datetime.seq,
                              function(x) format(x, "%Y-%m-%d %H:%M"))

y$datetime <- datetime.seq.string

write.table(y, file = 'r.txt',
            sep = ',', quote = FALSE, row.names = FALSE)


