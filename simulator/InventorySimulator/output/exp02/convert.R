# convert the input files java-lead-times-estimation-all.txt
# into the output files pgfplots-lead-times-estimation-all.txt
#
# this code converts lead times which are in units of simulation weeks,
# with 48 simulation weeks in a year, into calendar weeks, with 365 / 7
# calendar weeks in a year
#
# service level is a percentage, i.e., the max is 100.
#
# Running time: 80 seconds for 10^5 replications

rm(list = ls())

require(plyr)
require(dplyr)

Tic <- function(gcFirst = TRUE, type=c("elapsed", "user.self", "sys.self"))
{
  type <- match.arg(type)
  assign(".type", type, envir=baseenv())
  if(gcFirst) gc(FALSE)
  tic <- proc.time()[type]         
  assign(".tic", tic, envir=baseenv())
  invisible(tic)
}

Toc <- function()
{
  type <- get(".type", envir=baseenv())
  toc <- proc.time()[type]
  tic <- get(".tic", envir=baseenv())
  cat(sprintf("Time elapsed = %.1fs\n", toc - tic))
  invisible(toc)
}




ConvertFile <- function(the.type, the.par) {
  
  data.df <- read.table(sprintf('java-%s-%s-all.txt', the.par, the.type),
                        header = TRUE,
                        sep = '\t', comment.char = '#')
  # Convert the service level to a percentage
  data.df <- data.df %>%
      mutate(Service = 100 * Service)
  
  data.df <- ddply(data.df, .(LeadTime, Seasonality, Policy), summarise,
                   service = mean(Service),
                   service.sd = sd(Service),
                   mean.inv = mean(Inventory),
                   mean.inv.sd = sd(Inventory),
                   max.inv = mean(MaxInventory),
                   max.inv.sd = sd(MaxInventory),
                   reps = length(Service))
  names(data.df)[1] <- 'lead.time'
  names(data.df)[2] <- 'seasonality'
  names(data.df)[3] <- 'policy'
  # Convert the lead times from periods to weeks
  data.df$lead.time <- data.df$lead.time / 48 * 365 / 7
  # Convert the units from periods to months
  data.df$mean.inv <- data.df$mean.inv / 4
  data.df$max.inv <- data.df$max.inv / 4
  
  # Compute the error bars
  for (col in c('service', 'mean.inv', 'max.inv')) {
    data.df[sprintf('%s.error', col)] <-
      1.96 * data.df[sprintf('%s.sd', col)] / sqrt(data.df['reps'])
  }
  
  number.of.policies <- length(unique(data.df$policy))
  
  for (i in seq(0,number.of.policies - 1)) {
    write.table(round(subset(data.df, policy == i), 4),
                file = sprintf('pgfplots-%s-%s-%d.txt', the.par, the.type, i),
                sep = '\t',
                quote = FALSE,
                row.names = FALSE)
  }
  
}


Tic()

for (the.type in c('estimation', 'order')) {
  for (the.par in c('leadtime', 'seasonality')) {
    ConvertFile(the.type, the.par)
  }  
}

Toc()