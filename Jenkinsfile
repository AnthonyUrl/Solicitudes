// ============================================================================
//  Pipeline declarativo de Jenkins
//  Proyecto: Sistema de Solicitudes - QA Automatizado
//  Ejecuta la suite unitaria (JUnit Jupiter) y la suite de interfaz
//  (Selenium WebDriver), y publica los reportes y las capturas como evidencia.
//
//  Requisitos del agente: JDK 17+, Maven 3.9+ y Google Chrome instalado.
//  El pipeline detecta el sistema operativo: usa `sh` en Linux/macOS y `bat`
//  en Windows.
// ============================================================================

pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
    }

    // Si en su instalacion de Jenkins los tools estan configurados con nombre,
    // descomente y ajuste los nombres declarados en "Administrar Jenkins > Tools":
    //
    // tools {
    //     jdk 'jdk-17'
    //     maven 'maven-3.9'
    // }

    stages {

        stage('Codigo') {
            steps {
                echo 'Etapa 1: obteniendo el codigo fuente desde el repositorio (SCM)...'
                checkout scm
            }
        }

        stage('Compilar') {
            steps {
                echo 'Etapa 2: compilando y empaquetando el proyecto sin ejecutar pruebas...'
                mvn('clean package -DskipTests')
            }
        }

        stage('JUnit') {
            steps {
                echo 'Etapa 3: ejecutando las pruebas unitarias de la regla de negocio...'
                mvn('-Dtest=ValidadorSolicitudTest -Dsurefire.reports.dir=surefire-reports-junit test')
            }
        }

        stage('Selenium') {
            steps {
                echo 'Etapa 4: ejecutando las pruebas de interfaz web con Selenium WebDriver...'
                mvn('-Dtest=FlujoWebSeleniumTest -Dsurefire.reports.dir=surefire-reports-selenium test')
            }
        }

        stage('Evidencias') {
            steps {
                echo 'Etapa 5: archivando reportes XML y capturas generadas por las pruebas...'
                archiveArtifacts artifacts: 'target/surefire-reports-*/*.xml', allowEmptyArchive: true, fingerprint: true
                archiveArtifacts artifacts: 'target/screenshots/**/*.png', allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            echo 'Publicando los resultados de ambas suites en el reporte de pruebas de Jenkins...'
            junit testResults: 'target/surefire-reports-*/**/*.xml', allowEmptyResults: true
        }
        success {
            echo 'Pipeline completado: las suites unitaria y de interfaz finalizaron sin fallos.'
        }
        failure {
            echo 'Pipeline con fallos: revise la consola y el reporte JUnit para identificar la prueba que fallo.'
        }
    }
}

/**
 * Ejecuta Maven con los argumentos indicados, usando el comando adecuado
 * segun el sistema operativo del agente Jenkins.
 */
def mvn(String argumentos) {
    if (isUnix()) {
        sh "mvn -B ${argumentos}"
    } else {
        bat "mvn -B ${argumentos}"
    }
}
