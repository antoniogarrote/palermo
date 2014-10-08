(ns palermo.cli.utils
  (import [org.apache.commons.cli Option OptionBuilder Options HelpFormatter]))

(def ^:dynamic *defaults* (atom {}))

(defn make-option [name description default]
  (swap! *defaults* assoc name default)
  (new Option name description))

(defn make-value-option [name description arg-name default]
  (swap! *defaults* assoc name default)
  (do (OptionBuilder/withArgName arg-name)
      (OptionBuilder/hasArg)
      (OptionBuilder/withDescription description)
      (OptionBuilder/create name)))

(defn make-multi-value-option [name description arg-name default]
  (swap! *defaults* assoc name default)
  (do (OptionBuilder/withArgName arg-name)
      (OptionBuilder/hasArg)
      (OptionBuilder/withValueSeparator)
      (OptionBuilder/withDescription description)
      (OptionBuilder/create name)))

(defn make-options [& options]
  (let [opts (new Options)]
    (doseq [option options]
      (.addOption opts option))
    opts))

(defn value-for [cmd argument]
  (or (.getOptionValue cmd argument) (get (deref *defaults*) argument)))

(defn to-help [cmd options]
  (let [formatter (new HelpFormatter)]
    (.printHelp formatter cmd options)))
