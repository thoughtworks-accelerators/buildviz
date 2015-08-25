(defproject buildviz "0.2.0"
  :description "Transparency for your build pipeline's results and runtime."
  :url "https://github.com/cburgmer/buildviz"
  :license {:name "BSD 2-Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [ring/ring-json "0.2.0"]
                 [ring-middleware-accept "2.0.3"]
                 [compojure "1.1.6"]
                 [bigml/closchema "0.6.1"]
                 [com.taoensso/nippy "2.9.0"]]
  :plugins [[lein-ring "0.9.3"]
            [lein-exec "0.3.4"]
            [lein-npm "0.6.1"]]
  :npm {:dependencies [[d3 "3.5.5"]]
        :root "resources/public/js"}
  :ring {:handler buildviz.main/app
         :nrepl {:start? true
                 :port 9998}}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [cheshire "5.4.0"]
                                  [ring-mock "0.1.5"]]}})
