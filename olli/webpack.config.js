module.exports = {

    resolve: {
        extensions: ['.ts', '.js']
    },
    module: {
        rules: [
            { test: /\.ts$/, use: { loader: 'awesome-typescript-loader', options: { silent: true } } }
        ]
    },
    devtool: false,

    output: {
        filename: 'bundle.js',
        libraryTarget: 'var',
        library: 'main'
    },

    mode: 'development'

}
